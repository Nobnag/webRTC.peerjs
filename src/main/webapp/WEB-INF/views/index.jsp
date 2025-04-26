<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<link rel="shortcut icon" href="#" />
    <meta charset="UTF-8">
    <title>WebRTC working example</title>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.5.1/sockjs.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
    <style>
        #chatArea {
            border: 1px solid #ccc;
            height: 200px;
            overflow-y: scroll;
            padding: 10px;
            margin-top: 10px;
        }
        #chatInput {
            width: 80%;
            padding: 8px;
            margin-top: 5px;
        }
        #sendBtn {
            width: 15%;
            padding: 8px;
            margin-left: 5px;
        }
        #localStream{max-width: 100%; width: 500px;display: none;}
        #remoteStreamDiv{
	        width: 100%;
		    display: flex;
		    flex-wrap: wrap;
        }
        #startSteamBtn{display: none;}
        .subimg{max-width: 100%; width: 500px;display: none;}
        
    </style>
</head>
<body>
	<div>
		<input type="number" id="roomIdInput" placeholder="회의실 번호를 입력해주세요" max="999"/>
		<button type="button" id="enterRoomBtn">입장하기</button>
		<button type="button" id="startSteamBtn">화면 켜기</button>
	</div>
	
	<!-- 내 화면 엘리먼트 -->
	<div>
		<video id="localStream" autoplay playsinline controls></video>
		<div class="subimg">
			<img alt="프로필" src="" >
		</div>
		
	</div>

	<!-- 영상 받아올 엘리먼트 -->
	<div id="remoteStreamDiv"></div>

    <div class="message full"></div>

    <div id="chatArea"></div>
    <div>
        <input type="text" id="chatInput" placeholder="메시지를 입력하세요." />
        <button type="button" id="sendBtn">보내기</button>
    </div>

		<script src="peerConfig.js"></script>
</body>
</html>