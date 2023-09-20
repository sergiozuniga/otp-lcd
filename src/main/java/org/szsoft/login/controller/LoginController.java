package org.szsoft.login.controller;

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.szsoft.login.helper.EmailTemplate;
import org.szsoft.login.model.User;
import org.szsoft.login.service.EmailService;
import org.szsoft.login.service.UserService;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    public EmailService emailService;
    
    @GetMapping(value={"/", "/login"})
    public ModelAndView login(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        return modelAndView;
    }

    @GetMapping(value="/registration")
    public ModelAndView registration(){
        ModelAndView modelAndView = new ModelAndView();
        User user = new User();
        modelAndView.addObject("user", user);
        modelAndView.setViewName("registration");
        return modelAndView;
    }

    @PostMapping(value = "/registration")
    public ModelAndView createNewUser(@Valid User user) throws QrGenerationException {
        ModelAndView modelAndView = new ModelAndView();
        User userExists = userService.findUserByUserName(user.getUserName());
        if (userExists != null) {
            modelAndView.addObject("errorMessage", "Usuario ya existe");
            modelAndView.addObject("user", new User());
            modelAndView.setViewName("registration");
        } else {
            userService.saveUser(user);
            modelAndView.addObject("successMessage", "Usuario registrado de manera exitosa");
            modelAndView.addObject("user", new User());
            modelAndView.setViewName("registration");

        }
        return modelAndView;
    }

    @GetMapping(value="/admin/home")
    public ModelAndView home(){
        ModelAndView modelAndView = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByUserName(auth.getName());
        modelAndView.addObject("userName", user.getName() + " " + user.getLastName() + " - " + user.getFormatRut());
        modelAndView.addObject("qrcode", user.getQrCode());
        modelAndView.addObject("secret", user.getSecret());
        modelAndView.setViewName("admin/home");
        return modelAndView;
    }

    @GetMapping("/admin/sendemail")
    public ModelAndView sendEmail() throws MessagingException{
    	ModelAndView modelAndView = new ModelAndView();
    	try {
	    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    	User user = userService.findUserByUserName(auth.getName());
	    	EmailTemplate template = new EmailTemplate("sendemail.html");
			Map<String, String> replacements = new HashMap<String, String>();
			replacements.put("name", user.getName());
			replacements.put("lastname", user.getLastName());
			replacements.put("qrcode", user.getQrCode());
			String message = template.getTemplate(replacements);
			String to = user.getName() + " " + user.getLastName() + " <" + user.getEmail() + ">";
	    	emailService.sendEmailMessage(to, "Envío Código QR Usuario", message);
	        modelAndView.addObject("successMessage", "Email enviado de manera exitosa");
	        modelAndView.addObject("userName", user.getName() + " " + user.getLastName() + " - " + user.getFormatRut());
	        modelAndView.addObject("qrcode", user.getQrCode());
	        modelAndView.addObject("secret", user.getSecret());
    	}
    	catch(Exception e) {
            modelAndView.addObject("errorMessage", "Email no pudo ser enviado");    		
    	}
        modelAndView.setViewName("admin/home");
        return modelAndView;
    }
    
    @GetMapping(value="/admin/validation")
    public ModelAndView validation(){
        ModelAndView modelAndView = new ModelAndView();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findUserByUserName(auth.getName());
        //modelAndView.addObject("user", user);
        modelAndView.setViewName("/admin/validation");
        return modelAndView;
    }
    
    @PostMapping("/admin/validation")
    public ModelAndView validationOTP(@RequestParam(name = "otpcode") String otpcode){
    	ModelAndView modelAndView = new ModelAndView();
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	User user = userService.findUserByUserName(auth.getName());
    	TimeProvider timeProvider = new SystemTimeProvider();
    	CodeGenerator codeGenerator = new DefaultCodeGenerator();
    	CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    	boolean successful = verifier.isValidCode(user.getSecret(), otpcode);
    	boolean exito;
		if(successful) {
			modelAndView.addObject("successMessage", "Código OTP es válido");
			exito = true;
		}
		else {
			modelAndView.addObject("errorMessage", "Código OTP no es válido");
			exito = false;
		}
        modelAndView.addObject("userName", user.getName() + " " + user.getLastName() + " - " + user.getFormatRut());
        modelAndView.addObject("qrcode", user.getQrCode());
        modelAndView.addObject("secret", user.getSecret());
		modelAndView.addObject("exito", exito);
        modelAndView.setViewName("admin/home");
        return modelAndView;
    }

}
