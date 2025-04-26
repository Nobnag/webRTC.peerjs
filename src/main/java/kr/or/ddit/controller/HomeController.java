package kr.or.ddit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
	
	
	// 메인 페이지로 이동
	@RequestMapping(value = {"/", "/home"})
	public String home() {
		return "index";
	}
	
	// 웹소켓 페이지로 이동
	@GetMapping("/websocket")
	public String webSocket() {
		return "websocket";
	}
}
