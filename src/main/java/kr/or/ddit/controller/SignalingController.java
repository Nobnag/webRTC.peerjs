package kr.or.ddit.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
public class SignalingController {
	
	//offer 정보를 주고 받기 위한 websocket
	//camKey : 각 요청하는 캠의 key , roomId : 룸 아이디
	@MessageMapping("/peer/offer/{camKey}/{roomId}")
	@SendTo("/topic/peer/offer/{camKey}/{roomId}")
	public String PeerHandleOffer(@Payload String offer, @DestinationVariable(value = "roomId") String roomId,
			@DestinationVariable(value = "camKey") String camKey) {
		log.info("[OFFER] {} : {}", camKey, offer);
		return offer;
	}

	//iceCandidate 정보를 주고 받기 위한 webSocket
	//camKey : 각 요청하는 캠의 key , roomId : 룸 아이디
	@MessageMapping("/peer/iceCandidate/{camKey}/{roomId}")
	@SendTo("/topic/peer/iceCandidate/{camKey}/{roomId}")
	public Map<String, Object> PeerHandleIceCandidate(@Payload Map<String, Object> candidate, @DestinationVariable(value = "roomId") String roomId,
			@DestinationVariable(value = "camKey") String camKey) {
		log.info("[ICECANDIDATE] {} : {}", camKey, candidate);
		return candidate;
	}

	//
	@MessageMapping("/peer/answer/{camKey}/{roomId}")
	@SendTo("/topic/peer/answer/{camKey}/{roomId}")
	public String PeerHandleAnswer(@Payload String answer, @DestinationVariable(value = "roomId") String roomId,
			@DestinationVariable(value = "camKey") String camKey) {
		log.info("[ANSWER] {} : {}", camKey, answer);
		return answer;
	}

	//camKey 를 받기위해 신호를 보내는 webSocket
	@MessageMapping("/call/key")
	@SendTo("/topic/call/key")
	public String callKey(@Payload String message) {
		log.info("[Key] : {}", message);
		return message;
	}

	//자신의 camKey 를 모든 연결된 세션에 보내는 webSocket
	@MessageMapping("/send/key")
	@SendTo("/topic/send/key")
	public String sendKey(@Payload String message) {
		return message;
	}

	// 채팅 메시지 처리
	@MessageMapping("/chat/{roomId}")
	@SendTo("/topic/chat/{roomId}")
	public Map<String, String> handleChatMessage(@Payload Map<String, String> message, @DestinationVariable String roomId) {
		log.info("[CHAT] {} in room {}: {}", message.get("sender"), roomId, message.get("message"));
		return message;
	}
	
	private final Map<String, List<String>> roomParticipants = new ConcurrentHashMap<>();
	
	//입장 메시지 처리
	@MessageMapping("/chat/join/{roomId}")
    @SendTo("/topic/chat/join/{roomId}")
    public Map<String, String> joinChatMessage(@Payload Map<String, String> message, @DestinationVariable String roomId){
        String sender = message.get("sender");
        roomParticipants.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(sender);
        log.info("[CHAT] {} join room {}: {}", sender, roomId, message.get("message"));
        return message;
    }
	
	// 퇴장 메시지 처리
	@MessageMapping("/chat/leave/{roomId}")
    @SendTo("/topic/chat/leave/{roomId}")
    public Map<String, String> handleLeaveMessage(@Payload Map<String, String> message, @DestinationVariable String roomId) {
        String key = message.get("key");
        List<String> participants = roomParticipants.get(roomId);
        if (participants != null) {
            participants.remove(key);
            if (participants.isEmpty()) {
                roomParticipants.remove(roomId);
            }
        }
        log.info("[LEAVE] {} in room {}", key, roomId);
        Map<String, String> leaveMessage = new HashMap<>();
        leaveMessage.put("sender", key);
        leaveMessage.put("message", " 님이 퇴장하셨습니다.");
        return leaveMessage;
    }
	
	// 화면 켜짐 알림 처리
    @MessageMapping("/stream/start/{roomId}")
    @SendTo("/topic/stream/start/{roomId}")
    public Map<String, String> handleStreamStart(@Payload Map<String, String> message, @DestinationVariable String roomId) {
        String key = message.get("key");
        log.info("[STREAM START] {} in room {}", key, roomId);
        return message; // 켜짐 알림을 다른 클라이언트들에게 전달
    }
	
	/*
	 * // 스트림 종료 알림 처리
	 * 
	 * @MessageMapping("/stream/end/{roomId}")
	 * 
	 * @SendTo("/topic/stream/end/{roomId}") public Map<String, String>
	 * handleStreamEnd(@Payload Map<String, String> message, @DestinationVariable
	 * String roomId) { String key = message.get("key"); // (선택 사항) 스트림 종료 시
	 * roomParticipants에서 제거할지 여부 결정 // List<String> participants =
	 * roomParticipants.get(roomId); // if (participants != null) { //
	 * participants.remove(key); // } log.info("[STREAM END] {} in room {}", key,
	 * roomId); return message; // 종료 알림을 다른 클라이언트들에게 전달 }
	 */
}