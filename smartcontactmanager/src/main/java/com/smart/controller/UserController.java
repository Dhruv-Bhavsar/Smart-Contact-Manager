package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.ls.LSOutput;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		
		// get the user using username(Email)
		User user = this.userRepository.getUserByUserName(userName);
		
		System.out.println("user : " + user);
		model.addAttribute("user", user);
	}
	
	
	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	// open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,Principal principal, HttpSession session) {
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			// processing and uploading file
			
			if(file.isEmpty()) {
				// if file is empty then try out our message
				System.out.println("File is empty...Please upload an image");
				contact.setImage("contact.png");
			} else {
				// upload file in folder and update the name to contact
				
				contact.setImage(file.getOriginalFilename());
				
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image is uploaded successfully...");
			}
			
			
			
			user.getContacts().add(contact);
			contact.setUser(user);
			User user2 = this.userRepository.save(user);
			
			System.out.println("Data :" + contact);
			System.out.println("Added to database");
			
			// success message
			session.setAttribute("message", new Message("Your contact is added successfully! Add more..", "success"));
			
			
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("ERROR : " + e.getMessage());
			e.printStackTrace();
			
			//error message
			session.setAttribute("message", new Message("Something went wrong! please try again..", "danger"));
			
		}
		return "normal/add_contact_form";
	}
	
	
	// show contacts handler
	// per page = 5[n]
	// current page = 0 [page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show User Contacts");
		String name = principal.getName();
		
		User user = this.userRepository.getUserByUserName(name);
		
		Pageable pageable = PageRequest.of(page, 5);
		
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
		
		
		return "normal/show_contacts";
		
	}
	
	// showing particular contact details
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("CID : " + cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);	
			model.addAttribute("title", contact.getName());
		}
			
		
		return "normal/contact_detail";
	}
	
	// delete contact from database;
	@GetMapping("/delete/{cId}")
	public String  deleteContact(@PathVariable("cId") Integer cId, Model model, Principal principal, HttpSession session) {
		
		try {
			Contact contact = this.contactRepository.findById(cId).get();
			
			
			// check..
			
			
			String userName = principal.getName();
			User user = this.userRepository.getUserByUserName(userName);
			
			if(user.getId() == contact.getUser().getId()) {
				model.addAttribute("contact", contact);	
				contact.setUser(null);
				
				// delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, contact.getImage());
				file1.delete();
				
				user.getContacts().remove(contact);
				
				this.userRepository.save(user);
				session.setAttribute("message", new Message("Contact deleted Successfully...", "success"));
			}
		} catch (Exception e) {
			System.out.println("ERROR : " + e.getMessage());
			e.printStackTrace();
			
			//error message
			session.setAttribute("message", new Message("No contact found with this id..", "danger"));
		}
		
		
		
		return "redirect:/user/show-contacts/0";
	}
	
	// Open update form handler
	@PostMapping("/update-contact/{cId}")
	public String updateContact(@PathVariable("cId") Integer cId, Model model) {
		model.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cId).get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	// update contact handler
	@RequestMapping(value="/process-update", method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Model model, Principal principal,  HttpSession session) {
		
		try {
			
			Contact oldContact = this.contactRepository.findById(contact.getCid()).get();
			if(!file.isEmpty()) {
				// file upload
				// rewrite
				
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContact.getImage());
				file1.delete();
				
				
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
				
				
			} else {
				contact.setImage(oldContact.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Contact updated successfully...", "success"));
			
		} catch (Exception e) {
			session.setAttribute("message", new Message("Something went wrong ! Please try again...", "danger"));
			e.printStackTrace();
		}
		System.out.println("Name :" + contact.getName());
		System.out.println("Id : " + contact.getCid());
		return "redirect:/user/"+contact.getCid()+"/contact/";
	}
	
	// your profile page handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Your Profile");
		return "normal/profile";
	}
	
	// open settings handler
	@GetMapping("/settings")
	public String openSettings(Model model) {
		model.addAttribute("title", "Settings");
		return "normal/settings";
	}
	
	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		
		System.out.println("Old : " + oldPassword);
		System.out.println("new : " + newPassword);
		
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
			// change the password
			user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(user);
			
			session.setAttribute("message", new Message("Password Change successfully...", "success"));
		} else {
			session.setAttribute("message", new Message("Please enter correct Old password...", "danger"));
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}
	
	
	
}
