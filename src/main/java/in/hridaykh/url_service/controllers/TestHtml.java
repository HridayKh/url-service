package in.hridaykh.url_service.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TestHtml {

	private List<String> tasks = new ArrayList<>(List.of("Buy Milk", "Walk Dog"));

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("tasks", tasks);
		return "index";
	}

	@PostMapping("/add-task")
	public String addTask(@RequestParam String task, Model model) {
		tasks.add(task);
		model.addAttribute("tasks", tasks);
		return "index :: #task-list";
	}
}
