package com.cpt202.appointment_system.Controllers;

import java.security.Provider.Service;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cpt202.appointment_system.Common.Result;
import com.cpt202.appointment_system.Models.Appointment;
import com.cpt202.appointment_system.Models.Groomer;
import com.cpt202.appointment_system.Models.ServiceType;
import com.cpt202.appointment_system.Models.User;
import com.cpt202.appointment_system.Repositories.AppointmentRepo;
import com.cpt202.appointment_system.Repositories.GroomerRepo;
import com.cpt202.appointment_system.Services.GroomerService;
import com.cpt202.appointment_system.Services.UserService;
import com.cpt202.appointment_system.Services.ServiceTypeService;
import com.cpt202.appointment_system.Services.AppointmentService;

// @RestController
@Controller
// @RequestMapping("manager")
@RequestMapping("/maintain")
public class ManagerController {

    @Autowired
    private UserService userService;

    @Autowired
    private GroomerService groomerService;

    @Autowired
    private ServiceTypeService ServiceTypeService;

    @Autowired
    private AppointmentService AppointmentService;

    @Autowired
    private GroomerRepo GroomerRepo;

    @Autowired
    private AppointmentRepo AppointmentRepo;

    // @GetMapping("")
    // public String showMaintainPage(Model model) {
    // List<User> users = userService.listAllCustomers_M();
    // model.addAttribute("users", users);
    // List<Groomer> groomers = groomerService.listAllGroomers();
    // model.addAttribute("groomers", groomers);
    // List<ServiceType> services = ServiceTypeService.getAllServiceTypes();
    // model.addAttribute("serviceTypes", services);
    // List<Appointment> appointments = AppointmentService.getAllAppointments();
    // model.addAttribute("appointments", appointments);
    // // List<User> userList = userService.listAllCustomers_M();
    // // model.addAttribute("userList", userList);
    // // model.addAttribute("userSearchRst", new ArrayList<User>());
    // return "manager";
    // }
    @GetMapping("/maintainUser")
    public String showMaintainUserPage(Model model) {
        List<User> users = userService.listAllCustomers_M();
        model.addAttribute("users", users);
        return "MaintainUser";
    }

    // @GetMapping("/searchUser")
    // public String searchUser(Model model) {
    // model.addAttribute("keyword", new String());
    // return "MaintainUser";
    // }

    @PostMapping("/searchUser")
    public String searchUserByKey(Model model, @RequestParam("keyword") String keyword) {
        List<User> userSearchRst = userService.searchCustomerByName_M(keyword);
        model.addAttribute("users", userSearchRst);
        return "MaintainUser";
    }

    @GetMapping("/maintainGroomer")
    public String showMaintainGroomerPage(Model model) {
        List<Groomer> groomers = groomerService.listAllGroomers();
        model.addAttribute("groomers", groomers);
        return "MaintainGroomer";
    }

    @PostMapping("/deleteGroomer")
    public String deleteGroomer(@ModelAttribute Groomer groomer) {
        Integer gid = groomer.getGid();
        groomerService.deleteGroomerById(gid);
        return "redirect:/maintain/maintainGroomer";
    }

    // @PostMapping("/addGroomer")
    // public String addGroomer(@ModelAttribute Groomer groomer) {
    // groomerService.updateGroomer(groomer);
    // return "redirect:/maintain/maintainGroomer";
    // }

    @PostMapping("/updateGroomer")
    public String updateGroomer(@ModelAttribute Groomer groomer) {
        groomerService.updateGroomer(groomer);
        return "redirect:/maintain/maintainGroomer";
    }

    @PostMapping("/searchGroomer")
    public String searchGroomer(Model model, @RequestParam("keyword") String keyword) {
        List<Groomer> groomers = groomerService.searchGroomerByName_M(keyword);
        model.addAttribute("groomers", groomers);
        return "MaintainGroomer";
    }

    @GetMapping("/api/groomers")
    @ResponseBody
    public List<Map<String, Object>> listGroomersForMobile() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Groomer groomer : groomerService.listAllGroomers()) {
            int upcoming = 0;
            for (Appointment appointment : AppointmentRepo.findByGid(groomer.getGid())) {
                String status = appointment.getStatus();
                boolean cancelled = status != null && status.toLowerCase().contains("cancel");
                boolean future = appointment.getStartTime() != null && appointment.getStartTime().after(now);
                if (future && !cancelled) upcoming++;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gid", groomer.getGid());
            item.put("name", groomer.getName());
            item.put("gender", groomer.getGender());
            item.put("imageURL", groomer.getImageURL());
            item.put("rank", groomer.getRank());
            item.put("phoneNumber", groomer.getPhoneNumber());
            item.put("address", extractAddress(groomer.getDescription()));
            item.put("description", groomer.getDescription());
            item.put("title", groomer.getTitle());
            item.put("experienceYears", groomer.getExperienceYears());
            item.put("specialties", groomer.getSpecialties());
            item.put("acceptingAppointments",
                    groomer.getAcceptingAppointments() == null || groomer.getAcceptingAppointments());
            item.put("upcomingAppointmentCount", upcoming);
            result.add(item);
        }
        return result;
    }

    @PostMapping("/api/groomers/{gid}/accepting")
    @ResponseBody
    public Map<String, Object> setGroomerAcceptingStatus(
            @org.springframework.web.bind.annotation.PathVariable Integer gid,
            @RequestParam("accepting") boolean accepting) {
        Groomer groomer = GroomerRepo.findByGid(gid);
        if (groomer == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Không tìm thấy nhân viên.");
            return error;
        }
        groomer.setAcceptingAppointments(accepting);
        groomerService.updateGroomer(groomer);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gid", gid);
        result.put("acceptingAppointments", accepting);
        return result;
    }

    private String extractAddress(String description) {
        if (description == null) return "";
        for (String line : description.split("\\r?\\n")) {
            if (line.trim().startsWith("Địa chỉ:")) {
                return line.trim().replaceFirst("^Địa chỉ:\\s*", "").trim();
            }
        }
        return "";
    }

    @GetMapping("maintainServiceType")
    public String showMaintainServiceTypePage(Model model) {
        List<ServiceType> serviceTypes = ServiceTypeService.getAllServiceTypes();
        model.addAttribute("serviceTypes", serviceTypes);
        return "MaintainServiceType";
    }

    @PostMapping("/deleteServiceType")
    public String deleteServiceType(@ModelAttribute ServiceType serviceType) {
        Integer sid = serviceType.getSid();
        ServiceTypeService.deleteServiceTypeById(sid);
        return "redirect:/maintain/maintainServiceType";
    }

    @PostMapping("/updateServiceType")
    public String updateServiceType(@ModelAttribute ServiceType serviceType) {
        ServiceTypeService.updateServiceType(serviceType);
        return "redirect:/maintain/maintainServiceType";
    }

    @PostMapping("/searchServiceType")
    public String searchServiceType(Model model, @RequestParam("keyword") String keyword) {
        List<ServiceType> serviceTypes = ServiceTypeService.searchServiceTypeByName_M(keyword);
        model.addAttribute("serviceTypes", serviceTypes);
        return "MaintainServiceType";
    }

    @GetMapping("maintainAppointment")
    public String showMaintainAppointmentPage(Model model) {
        List<Appointment> appointments = AppointmentService.getAllAppointments();
        model.addAttribute("appointments", appointments);
        return "MaintainAppointment";
    }

    @PostMapping("/updateAppointment")
    public String updateAppointment(@ModelAttribute Appointment appointment) {
        // AppointmentRepo.updateStatusByAid(appointment.getStatus(),
        // appointment.getAid());
        System.out.println(appointment.getStatus());
        AppointmentService.updateAppointment(appointment);

        return "redirect:/maintain/maintainAppointment";
    }

    @PostMapping("/searchAppointment")
    public String searchAppointment(Model model, @RequestParam("keyword") String keyword) {
        List<Appointment> Appointments = AppointmentService.appointmentSearch_M(keyword);
        model.addAttribute("appointments", Appointments);
        return "MaintainAppointment";
    }

    // @PostMapping("/addServiceType")
    // public String addServiceType(@ModelAttribute ServiceType service) {
    // ServiceTypeService.updateServiceType(service);
    // return "redirect:/maintain/maintainServiceType";
    // }

    // @GetMapping("/delete")
    // public Result<?> deleteUser1(@RequestParam("uid") Integer uid) {
    // userService.deleteUserById(uid);
    // // return "redirect:/appointment-system/manager";
    // return Result.success();
    // }

    // Manager can delete customer by id
    // @PostMapping("/delete")
    // public Result<?> deleteUser(@RequestParam("uid") Integer uid) {
    // userService.deleteUserById(uid);
    // // return "redirect:/appointment-system/manager";
    // return Result.success();
    // }

    // @GetMapping("/delete")
    // public String deleteTU(Model model) {
    // model.addAttribute("user", new User());
    // return "manager";
    // }

    // // for testing
    // @PostMapping("/deleteUser")
    // public String deleteUser(@ModelAttribute User user) {
    // Integer uid = user.getUid();
    // userService.deleteUserById(uid);
    // return "redirect:/manager";
    // // return Result.success();
    // }

    // @PostMapping("/searchUser")
    // public String searchUserByKey(@ModelAttribute("searchedUser") User
    // searchedUser, Model model) {
    // String searchedName = searchedUser.getUsername();
    // List<User> userSearchRst = userService.searchCustomerByName_M(searchedName);
    // model.addAttribute("userSearchRst", userSearchRst);
    // return "redirect:/manager";
    // // return Result.success();
    // }

    // @GetMapping("/manager/appointmentList/search")
    // public String appointmentSearch(Model model) {
    // model.addAttribute("keyword", new String());
    // return "allAppointments";
    // }

    // @PostMapping("/manager/appointmentList/search")
    // public String appointmentSearchBykey(Model model, @RequestParam("keyword")
    // String keyword) {
    // model.addAttribute("appointmentList",
    // appointmentService.appointmentSearch(keyword));
    // return "allAppointments";
    // }
    // @PostMapping("/user")
    // public String updateUser(@ModelAttribute("user") User user,
    // RedirectAttributes redirectAttributes) {
    // userService.updateUser(user);
    // redirectAttributes.addFlashAttribute("successMessage", "User updated
    // successfully");
    // return "redirect:/manager/maintain";
    // }

    // @PostMapping("/manager")
    // public String updateGroomer(@ModelAttribute("groomer") Groomer
    // groomer,RedirectAttributes redirectAttributes) {
    // // Groomer groomer1 = GroomerRepo.findByGid(gid);
    // groomerService.editGroomer_M(groomer);

    // redirectAttributes.addFlashAttribute("successMessage", "Groomer updated
    // successfully");
    // return "redirect:/manager";
    // }

    // @PostMapping("/serviceType")
    // public String updateServiceType(@ModelAttribute("services") ServiceType
    // service, RedirectAttributes redirectAttributes) {
    // ServiceTypeService.editServiceType(service);
    // redirectAttributes.addFlashAttribute("successMessage", "Service updated
    // successfully");
    // return "redirect:/manager";
    // }

    @GetMapping("/report")
    public String showReport(Model model,
            @RequestParam(value = "year", required = false, defaultValue = "2023") int year) {
        model.addAttribute("appointments", AppointmentService.findAllSale());
        model.addAttribute("annualAppointments", AppointmentService.getAnnualStaticalReport());
        model.addAttribute("quarterlyAppointments", AppointmentService.getQuarterlyStaticalReport(year));

        return "report";
    }

}