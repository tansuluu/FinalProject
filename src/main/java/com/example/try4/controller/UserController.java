package com.example.try4.controller;

import com.example.try4.dao.AppUserDAO;
import com.example.try4.dao.ApplicationDAO;
import com.example.try4.dao.PostDAO;
import com.example.try4.entity.AppUser;
import com.example.try4.entity.Application;
import com.example.try4.entity.Post;
import com.example.try4.service.EmailService;
import com.example.try4.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@Transactional
public class UserController {

    @Autowired
    private EmailService emailService;

    @Autowired
    StorageService storageService;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    private AppUserDAO appUserDAO;

    @Autowired
    private PostDAO postDAO;

    @RequestMapping("/userPage")
    public String showUser(Model model, @RequestParam("username")String username){
        AppUser user=appUserDAO.findAppUserByUserName(username);
        List<Application> list=applicationDAO.getUsersApplication(username);
        model.addAttribute("apps",list);
        model.addAttribute("user",user);
        model.addAttribute("post",new Post());
        model.addAttribute("posts",postDAO.findComment(username));
        return "userPage";
    }
    @RequestMapping(value = "/newPost", method = RequestMethod.POST)
    public String saveComment(@ModelAttribute("post") @Valid Post post, BindingResult result, Principal principal, @RequestParam("username") String username){
        if (result.hasErrors()) {
            return "redirect:/userPage?username="+username;
        }
        post.setUsername(principal.getName());
        post.setUser(username);
        post.setImage(appUserDAO.findAppUserByUserName(principal.getName()).getUrlImage());
        postDAO.addPost(post);
        return "redirect:/userPage?username="+username;
    }
    @RequestMapping("/deletePost")
    public String deletePost(@RequestParam("id") long id,@RequestParam("username") String username){
        postDAO.deletePost(id);
        return "redirect:/userPage?username="+username;
    }

    @RequestMapping("/deleteUser")
    public String deleteUser(@RequestParam("id") long id){
        appUserDAO.delete(id);
        return "redirect:/";
    }

    @RequestMapping("/updateUser")
    public String updateUser(@RequestParam("id") long id,Model model){
        model.addAttribute("user",appUserDAO.findAppUserByUserId(id));
        return "userUp";
    }
    @RequestMapping(value = "/updateUser",method =RequestMethod.POST)
    public String updateUsera(@ModelAttribute("user") @Valid AppUser user){
        System.out.println(user.getDepartment());
        appUserDAO.updateUser(user);
        return "redirect:/userPage?username="+user.getUserName();
    }
    @RequestMapping(value = "/forgot", method = RequestMethod.GET)
    public String displayForgotPasswordPage() {
        return "forget";
    }
    @RequestMapping(value = "/forgot", method = RequestMethod.POST)
    public String processForgotPasswordForm(Model modelAndView, @RequestParam("email") String userEmail, HttpServletRequest request) {

        AppUser user=appUserDAO.findByEmail(userEmail);

        if (user==null) {
            modelAndView.addAttribute("message", "We didn't find an account for that e-mail address."+user.getEmail());
            return "forget";
        } else {

            user.setConfirm(UUID.randomUUID().toString());

            // Save token to database
            appUserDAO.addUser(user);

            String appUrl = request.getScheme() + "://" + request.getServerName();

            // Email message
            SimpleMailMessage passwordResetEmail = new SimpleMailMessage();
            passwordResetEmail.setFrom("noreply@domain.com");
            passwordResetEmail.setTo(user.getEmail());
            passwordResetEmail.setSubject("Password Reset Request");
            passwordResetEmail.setText("To reset your password, click the link below:\n" + appUrl
                    + "/reset?token=" + user.getConfirm());

            emailService.sendEmail(passwordResetEmail);

            // Add success message to view
            modelAndView.addAttribute("message", "A password reset link has been sent to " + userEmail);
        }
;
        return "forget";

    }

    // Display form to reset password
    @RequestMapping(value = "/reset", method = RequestMethod.GET)
    public String  displayResetPasswordPage(Model modelAndView, @RequestParam("token") String token) {

        AppUser user=appUserDAO.findUserconfirm(token);

        if (user!=null) { // Token found in DB

        } else { // Token not found in DB
            modelAndView.addAttribute("message", "Oops!  This is an invalid password reset link.");
        }


        return "resetPassword";
    }
//
//    // Process reset password form
//    @RequestMapping(value = "/reset", method = RequestMethod.POST)
//    public ModelAndView setNewPassword(ModelAndView modelAndView, @RequestParam("password"), String ) {
//
//        // Find the user associated with the reset token
//        Optional<User> user = userService.findUserByResetToken(requestParams.get("token"));
//
//        // This should always be non-null but we check just in case
//        if (user.isPresent()) {
//
//            User resetUser = user.get();
//
//            // Set new password
//            resetUser.setPassword(bCryptPasswordEncoder.encode(requestParams.get("password")));
//
//            // Set the reset token to null so it cannot be used again
//            resetUser.setResetToken(null);
//
//            // Save user
//            userService.saveUser(resetUser);
//
//            // In order to set a model attribute on a redirect, we must use
//            // RedirectAttributes
//            redir.addFlashAttribute("successMessage", "You have successfully reset your password.  You may now login.");
//
//            modelAndView.setViewName("redirect:login");
//            return modelAndView;
//
//        } else {
//            modelAndView.addObject("errorMessage", "Oops!  This is an invalid password reset link.");
//            modelAndView.setViewName("resetPassword");
//        }
//
//        return modelAndView;
//    }


}
