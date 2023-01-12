package com.smart.controller;


import java.security.Principal;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
public class HomeController {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@GetMapping("/test")
	@ResponseBody
	public String test() {
		User user = new User();
		
		Contact contact = new Contact();
		user.setName("Avani Sanghvi");
		user.setEmail("avani@gmail.com");
		user.getContacts().add(contact);
		userRepository.save(user);
		
		
		return "Working";
	}
	
	@RequestMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home - Smart Contact Manager");
		return "home";
	}
	
	@RequestMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About - Smart Contact Manager");
		return "about";
	}
	
	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "Signup - Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}
	
	// handler for registering user
	@RequestMapping(value="/do_register", method = RequestMethod.POST)
	public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result1, @RequestParam(value="agreement", defaultValue="false") boolean agreement, Model model, HttpSession session) {
		
		try {
			if(!agreement) {
				System.out.println("You have not accept terms and conditions");
				throw new Exception("You have not accept terms and conditions");
			}
			
			if(result1.hasErrors()) {
				System.out.println("ERROR : " + result1.toString());
				model.addAttribute("user", user);
				return "signup";
			}
			
			user.setEnabled(true);
			user.setRole("ROLE_USER");
			user.setImageUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			System.out.println("agreement :" + agreement);
			System.out.println("user :" + user);
			
			
			User result = this.userRepository.save(user);
			model.addAttribute("user", new User());
			
			session.setAttribute("message", new Message("Successfully Registered!!" , "alert-success"));
			return "signup";
			
			
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("Something Went wrong !!" + e.getMessage(), "alert-danger"));
			return "signup";
		}
		
	}

	// handler for custom login
	@GetMapping("/signin")
	public String customLogin(Model model) {
		model.addAttribute("title", "Login - Smart Contact Manager");
		
		return "login";
	}
	
	// do login
	@PostMapping("/dologin")
	public String doLogin(@RequestParam("password") String password, Principal principal, HttpSession session) {
		
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		
		if(this.passwordEncoder.matches(password, user.getPassword())) {
			
			return "redirect:/user/index";
		} else {
			session.setAttribute("message", new Message("Please Enter Correct Password...", "danger"));
			return "redirect:/signin";
		}
		
		
		
		
	}
}
