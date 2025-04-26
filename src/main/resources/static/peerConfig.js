// let remoteStreamElement = document.querySelector('#remoteStream');
let localStreamElement = document.querySelector('#localStream');
//const myKey = Math.random().toString(36).substring(2, 11);
let myKey = "";
let pcListMap = new Map();
let username = "";
let roomId;
let otherKeyList = [];
let localStream = undefined;
let stompClient = null; // stompClient 변수를 전역으로 선언

const chatArea = document.querySelector('#chatArea');
const chatInput = document.querySelector('#chatInput');
const sendBtn = document.querySelector('#sendBtn');

// 채팅 이름 설정-----------
// 회원 세션으로 변경 할것
const fn_setname = () => {
	myKey = prompt("이름을 설정해주세요");
	if(myKey == "" || myKey == null){
		alert("이름을 설정해주세요");
		//회원이름 설정하지않으면 다시
		fn_setname();
	}
}
fn_setname();
// 채팅 이름 설정----------- end

const startCam = async () =>{
    if(navigator.mediaDevices !== undefined){
        await navigator.mediaDevices.getUserMedia({ audio: true, video : true })
            .then(async (stream) => {
                console.log('Stream found');
				console.log(stream);
				//웹캠, 마이크의 스트림 정보를 글로벌 변수로 저장한다.
                localStream = stream;
                // Disable the microphone by default
				//음향
                stream.getAudioTracks()[0].enabled = true;
                localStreamElement.srcObject = localStream;
                // Connect after making sure that local stream is availble

            }).catch(error => {
                console.error("Error accessing media devices:", error);
            });
    }

}

// 소켓 연결
const connectSocket = async () =>{
    const socket = new SockJS('/signaling');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        console.log('Connected to WebRTC server');

		//iceCandidate peer 교환을 위한 subscribe
        stompClient.subscribe(`/topic/peer/iceCandidate/${myKey}/${roomId}`, candidate => {
            const key = JSON.parse(candidate.body).key
            const message = JSON.parse(candidate.body).body;

			// 해당 key에 해당되는 peer 에 받은 정보를 addIceCandidate 해준다.
            pcListMap.get(key).addIceCandidate(new RTCIceCandidate({candidate:message.candidate,sdpMLineIndex:message.sdpMLineIndex,sdpMid:message.sdpMid}));

        });

		//offer peer 교환을 위한 subscribe
        stompClient.subscribe(`/topic/peer/offer/${myKey}/${roomId}`, offer => {
            const key = JSON.parse(offer.body).key;
            const message = JSON.parse(offer.body).body;

			// 해당 key에 새로운 peerConnection 를 생성해준후 pcListMap 에 저장해준다.
            pcListMap.set(key,createPeerConnection(key));
			// 생성한 peer 에 offer정보를 setRemoteDescription 해준다.
            pcListMap.get(key).setRemoteDescription(new RTCSessionDescription({type:message.type,sdp:message.sdp}));
            //sendAnswer 함수를 호출해준다.
			sendAnswer(pcListMap.get(key), key);

        });

		//answer peer 교환을 위한 subscribe
        stompClient.subscribe(`/topic/peer/answer/${myKey}/${roomId}`, answer =>{
            const key = JSON.parse(answer.body).key;
            const message = JSON.parse(answer.body).body;

			// 해당 key에 해당되는 Peer 에 받은 정보를 setRemoteDescription 해준다.
            pcListMap.get(key).setRemoteDescription(new RTCSessionDescription(message));

        });

 		//key를 보내라는 신호를 받은 subscribe
        stompClient.subscribe(`/topic/call/key`, message =>{
			//자신의 key를 보내는 send
	        stompClient.send(`/app/send/key`, {}, JSON.stringify(myKey));
        });

		//상대방의 key를 받는 subscribe
        stompClient.subscribe(`/topic/send/key`, message => {
            const key = JSON.parse(message.body);
			//만약 중복되는 키가 ohterKeyList에 있는지 확인하고 없다면 추가해준다.
            if(myKey !== key && otherKeyList.find((mapKey) => mapKey === myKey) === undefined){
                otherKeyList.push(key);
            }
        });

        // 채팅 메시지 수신
        stompClient.subscribe(`/topic/chat/${roomId}`, message => {
            const receivedMessage = JSON.parse(message.body);
            displayChatMessage(`${receivedMessage.sender}` ,`${receivedMessage.message}`);
        });

    });
}

let onTrack = (event, otherKey) => {
    if(document.getElementById(`${otherKey}`) === null){
        const video =  document.createElement('video');

        video.autoplay = true;
        video.controls = true;
        video.id = otherKey;
        video.srcObject = event.streams[0];

        document.getElementById('remoteStreamDiv').appendChild(video);
    }

    // remoteStreamElement.srcObject = event.streams[0];
    // remoteStreamElement.play();
};

const createPeerConnection = (otherKey) =>{
    const pc = new RTCPeerConnection();
    try {
        pc.addEventListener('icecandidate', (event) =>{
            onIceCandidate(event, otherKey);
        });
        pc.addEventListener('track', (event) =>{
            onTrack(event, otherKey);
        });
        if(localStream !== undefined){
            localStream.getTracks().forEach(track => {
                pc.addTrack(track, localStream);
            });
        }

        console.log('PeerConnection created');
    } catch (error) {
        console.error('PeerConnection failed: ', error);
    }
    return pc;
}

let onIceCandidate = (event, otherKey) => {
    if (event.candidate) {
        console.log('ICE candidate');
        stompClient.send(`/app/peer/iceCandidate/${otherKey}/${roomId}`,{}, JSON.stringify({
            key : myKey,
            body : event.candidate
        }));
    }
};

let sendOffer = (pc ,otherKey) => {
    pc.createOffer().then(offer =>{
        setLocalAndSendMessage(pc, offer);
        stompClient.send(`/app/peer/offer/${otherKey}/${roomId}`, {}, JSON.stringify({
            key : myKey,
            body : offer
        }));
        console.log('Send offer');
    });
};

let sendAnswer = (pc,otherKey) => {
    pc.createAnswer().then( answer => {
        setLocalAndSendMessage(pc ,answer);
        stompClient.send(`/app/peer/answer/${otherKey}/${roomId}`, {}, JSON.stringify({
            key : myKey,
            body : answer
        }));
        console.log('Send answer');
    });
};

// 채팅 메시지 표기
const displayChatMessage = (sender, message) => {
	/* 제이쿼리
	    let chatMessage = "";
		chatMessage += "<div>"+sender+" : "+message+"</div>";
	*/
	let ele = document.createElement('div');
	let chatMessage = `${sender} : ${message}`
	ele.innerHTML = chatMessage;
	
	let senderCheck = myKey == sender ? "mymessage" : "send"
	ele.className = senderCheck
	
    chatArea.appendChild(ele);
    chatArea.scrollTop = chatArea.scrollHeight; // 스크롤 하단으로 이동
};
//메시지 보내기
const sendChatMessage = () => {
    const message = chatInput.value.trim();
	
	if(stompClient == "" || stompClient == null){
		alert("회의실번호를 입력해주세요");
		document.querySelector('#roomIdInput').focus();
		return false;
	}
	
    if (message) {
        stompClient.send(`/app/chat/${roomId}`, {}, JSON.stringify({
            sender: myKey,
            message: message
        }));
        chatInput.value = '';
    }
};

const setLocalAndSendMessage = (pc ,sessionDescription) =>{
    pc.setLocalDescription(sessionDescription);
};

// 채팅 메시지 전송 이벤트 리스너
sendBtn.addEventListener('click', sendChatMessage);
chatInput.addEventListener('keypress', (event) => {
    if (event.key === 'Enter') {
        sendChatMessage();
    }
});

//룸 번호 입력 후 캠 + 웹소켓 실행
document.querySelector('#enterRoomBtn').addEventListener('click', async () =>{
    await startCam();

    if(localStream !== undefined){
        document.querySelector('#localStream').style.display = 'block';
        document.querySelector('#startSteamBtn').style.display = 'block';
    }
    roomId = document.querySelector('#roomIdInput').value;
    document.querySelector('#roomIdInput').disabled = true;
    document.querySelector('#enterRoomBtn').disabled = true;
	if(roomId == "" || roomId == null){
		alert("회의실번호를 입력해주세요");
		return false
	}

    await connectSocket();
});

// 스트림 버튼 클릭시 , 다른 웹 key들 웹소켓을 가져 온뒤에 offer -> answer -> iceCandidate 통신
// peer 커넥션은 pcListMap 으로 저장
document.querySelector('#startSteamBtn').addEventListener('click', async () =>{
    await stompClient.send(`/app/call/key`, {}, {});

    setTimeout(() =>{

        otherKeyList.map((key) =>{
            if(!pcListMap.has(key)){
                pcListMap.set(key, createPeerConnection(key));
                sendOffer(pcListMap.get(key),key);
            }

        });

    },1000);
});