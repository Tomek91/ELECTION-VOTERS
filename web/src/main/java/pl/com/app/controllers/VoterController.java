package pl.com.app.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.com.app.dto.CandidateDTO;
import pl.com.app.dto.ConstituencyDTO;
import pl.com.app.dto.ResultDTO;
import pl.com.app.dto.VoterDTO;
import pl.com.app.exceptions.ExceptionCode;
import pl.com.app.exceptions.MyException;
import pl.com.app.model.enums.ETour;
import pl.com.app.service.CandidateService;
import pl.com.app.service.ConstituencyService;
import pl.com.app.service.ResultService;
import pl.com.app.service.VoterService;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/voters")
public class VoterController {
    private final VoterService voterService;
    private final ConstituencyService constituencyService;
    private final CandidateService candidateService;
    private final ResultService resultService;

    @PostMapping("/vote")
    public String vote(@RequestParam Long voterId,
                       @RequestParam Long constituencyId,
                       @RequestParam String voteApiName,
                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("voterId", voterId);
        redirectAttributes.addFlashAttribute("constituencyId", constituencyId);
        return "redirect:/voters/vote/" + voteApiName;
    }

    @GetMapping("/vote/first-tour")
    public String voteFirstTour(Model model) {
        Map<String, Object> modelMap = model.asMap();
        Long voterId = (Long) modelMap.get("voterId");
        Long constituencyId = (Long) modelMap.get("constituencyId");
        if (constituencyId == null || voterId == null){
            throw new MyException(ExceptionCode.VALIDATION, "YOU DON'T HAVE ACCESS TO THIS RESOURCE");
        }
        ConstituencyDTO constituencyDTO = constituencyService.getOneConstituency(constituencyId);
        model.addAttribute("constituency", constituencyDTO.getName());
        model.addAttribute("voterId", voterId);
        model.addAttribute("candidates",
                resultService
                        .findResultCandidatesByTour(ETour.FIRST_TOUR)
                        .stream()
                        .filter(x -> x.getCandidateDTO().getConstituencyDTO().equals(constituencyDTO))
                        .map(ResultDTO::getCandidateDTO)
                        .collect(Collectors.toList()));
        return "voters/voteFirstTour";
    }

    @PostMapping("/{voterId}/constituency/{constituencyId}/first-tour/vote/{candidateId}")
    public String voteFirstTour(@PathVariable String voterId,
                                @PathVariable Long constituencyId,
                                @PathVariable Long candidateId) {
        ConstituencyDTO constituencyDTO = constituencyService.getOneConstituency(constituencyId);
        CandidateDTO candidateDTO = candidateService.getOneCandidate(candidateId);
        resultService.addVoteFirstTour(constituencyDTO, candidateDTO);
        voterService.modifyVoter(VoterDTO
                .builder()
                .id(Long.valueOf(voterId))
                .constituencyDTO(constituencyDTO)
                .isVoteInFirstTour(Boolean.TRUE)
                .build());
        return "redirect:/";
    }

    @GetMapping("/vote/second-tour")
    public String voteSecondTour(Model model) {
        Map<String, Object> modelMap = model.asMap();
        Long voterId = (Long) modelMap.get("voterId");
        Long constituencyId = (Long) modelMap.get("constituencyId");
        if (constituencyId == null || voterId == null){
            throw new MyException(ExceptionCode.VALIDATION, "YOU DON'T HAVE ACCESS TO THIS RESOURCE");
        }
        ConstituencyDTO constituencyDTO = constituencyService.getOneConstituency(constituencyId);
        model.addAttribute("constituency", constituencyDTO.getName());
        model.addAttribute("voterId", voterId);
        model.addAttribute("candidates",
                resultService
                        .findResultCandidatesByTour(ETour.SECOND_TOUR)
                        .stream()
                        .filter(x -> x.getCandidateDTO().getConstituencyDTO().equals(constituencyDTO))
                        .map(ResultDTO::getCandidateDTO)
                        .collect(Collectors.toList()));
        return "voters/voteSecondTour";
    }

    @PostMapping("/{voterId}/constituency/{constituencyId}/second-tour/vote/{candidateId}")
    public String voteSecondTour(@PathVariable String voterId,
                                 @PathVariable Long constituencyId,
                                 @PathVariable Long candidateId) {
        ConstituencyDTO constituencyDTO = constituencyService.getOneConstituency(constituencyId);
        CandidateDTO candidateDTO = candidateService.getOneCandidate(candidateId);
        resultService.addVoteSecondTour(constituencyDTO, candidateDTO);
        voterService.modifyVoter(VoterDTO
                .builder()
                .id(Long.valueOf(voterId))
                .constituencyDTO(constituencyDTO)
                .isVoteInSecondTour(Boolean.TRUE)
                .build());
        return "redirect:/";
    }

    @GetMapping("/verify-voter")
    public String verifyVoter(Model model) {
        Map<String, Object> modelMap = model.asMap();
        Integer tourNumber = (Integer) modelMap.get("tourNumber");
        Long voterId = (Long) modelMap.get("voterId");
        if (tourNumber == null || voterId == null){
            throw new MyException(ExceptionCode.VALIDATION, "YOU DON'T HAVE ACCESS TO THIS RESOURCE");
        }
        model.addAttribute("voter", voterService.getOneVoter(voterId));
        model.addAttribute("voteApiName", tourNumber == 1 ? "first-tour" : "second-tour");
        return "voters/verify";
    }
}
