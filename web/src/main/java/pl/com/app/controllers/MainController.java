package pl.com.app.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.com.app.dto.AuthDTO;
import pl.com.app.model.enums.ETour;
import pl.com.app.service.ElectionService;
import pl.com.app.service.VoterService;
import pl.com.app.validators.AuthValidator;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
public class MainController {

    private final ElectionService electionService;
    private final VoterService voterService;
    private final AuthValidator authValidator;

    @InitBinder
    private void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.setValidator(authValidator);
    }

    @GetMapping({"/", ""})
    public String welcome(Model model) {
        model.addAttribute("auth", new AuthDTO());
        model.addAttribute("electionFirstTour", electionService.isActiveElection(ETour.FIRST_TOUR));
        model.addAttribute("electionSecondTour", electionService.isActiveElection(ETour.SECOND_TOUR));
        model.addAttribute("electionFirstTourResults", electionService.isEndElection(ETour.FIRST_TOUR));
        model.addAttribute("electionSecondTourResults", electionService.isEndElection(ETour.SECOND_TOUR));
        model.addAttribute("errors", new HashMap<>());
        model.addAttribute("errorsGlobal", new ArrayList<>());
        return "index";
    }

    @PostMapping({"/", ""})
    public String welcome(@Valid @ModelAttribute AuthDTO authDTO,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        boolean electionSecondTour = electionService.isActiveElection(ETour.SECOND_TOUR);
        if (bindingResult.hasErrors()) {
            model.addAttribute("auth", authDTO);
            model.addAttribute("electionFirstTour", electionService.isActiveElection(ETour.FIRST_TOUR));
            model.addAttribute("electionSecondTour", electionSecondTour);
            model.addAttribute("electionFirstTourResults", electionService.isEndElection(ETour.FIRST_TOUR));
            model.addAttribute("electionSecondTourResults", electionService.isEndElection(ETour.SECOND_TOUR));
            model.addAttribute("errorsGlobal", bindingResult
                    .getGlobalErrors()
                    .stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.toList()));
            model.addAttribute("errors", bindingResult
                    .getFieldErrors()
                    .stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)));
            return "index";
        }
        Long voterId = voterService.findVoterByPesel(authDTO.getPesel()).get().getId();
        redirectAttributes.addFlashAttribute("voterId", voterId);
        redirectAttributes.addFlashAttribute("tourNumber", (electionSecondTour ? 2 : 1));
        return "redirect:/voters/verify-voter";
    }
}
