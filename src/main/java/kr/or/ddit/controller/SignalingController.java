package kr.or.ddit.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
}