package pl.com.app.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.com.app.dto.*;
import pl.com.app.exceptions.ExceptionCode;
import pl.com.app.exceptions.MyException;
import pl.com.app.model.Candidate;
import pl.com.app.model.Result;
import pl.com.app.model.enums.ETour;
import pl.com.app.repository.CandidateRepository;
import pl.com.app.repository.ResultRepository;
import pl.com.app.service.mappers.ModelMapper;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ResultService {
    private final ResultRepository resultRepository;
    private final CandidateRepository candidateRepository;
    private final ModelMapper modelMapper;
    private final VoterService voterService;

    public void saveCandidates(ETour eTour, List<CandidateDTO> candidateDTOList) {
        try {
            if (eTour == null) {
                throw new NullPointerException("ETOUR IS NULL");
            }

            if (candidateDTOList == null || candidateDTOList.isEmpty()){
                throw new NullPointerException("CANDIDATE LIST IS NULL");
            }

            for (CandidateDTO candidateDTO : candidateDTOList){
                ResultDTO resultDTO = ResultDTO.builder()
                        .eTour(eTour)
                        .votes_number(0)
                        .candidateDTO(candidateDTO)
                        .build();
                resultRepository.saveOrUpdate(modelMapper.fromResultDTOToResult(resultDTO));
            }
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    public List<ResultDTO> findResultCandidatesByTour(ETour eTour) {
        try {
            if (eTour == null) {
                throw new NullPointerException("ETOUR IS NULL");
            }

            return resultRepository.findAll()
                    .stream()
                    .map(modelMapper::fromResultToResultDTO)
                    .filter(x -> x.getETour().equals(eTour))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    public List<CandidateDTO> findCandidatesForSecondTour() {
        try {
            return this.findResultsByTour(ETour.FIRST_TOUR)
                    .values()
                    .stream()
                    .filter(x -> x.stream().noneMatch(l -> l.getNumberOfVotesInPercent().compareTo(new BigDecimal(50)) > 0))
                    .map(x -> x.stream().limit(2).collect(Collectors.toList()))
                    .flatMap(List::stream)
                    .map(CandidateResultDTO::getCandidateDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    private BigDecimal calculateAverageResult(Integer size, Integer numberOfVotes){
        BigDecimal avgResult = BigDecimal.ZERO;
        if (size > 0){
            avgResult = new BigDecimal(numberOfVotes * 100).divide(new BigDecimal(size), 2, BigDecimal.ROUND_CEILING);
        }
        return avgResult;
    }

    public Map<ConstituencyDTO, List<CandidateResultDTO>> findResultsByTour(ETour eTour) {
        try {
            if (eTour == null) {
                throw new NullPointerException("ETOUR IS NULL");
            }

            List<ResultDTO> resultDTOList = this.findResultCandidatesByTour(eTour);
            if (resultDTOList.isEmpty()) {
                throw new NullPointerException("RESULT LIST IS NULL");
            }

            return resultDTOList
                    .stream()
                    .collect(Collectors.groupingBy(e -> e.getCandidateDTO().getConstituencyDTO()))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue()
                                    .stream()
                                    .map(x -> CandidateResultDTO
                                            .builder()
                                            .candidateDTO(x.getCandidateDTO())
                                            .numberOfVotes(x.getVotes_number())
                                            .numberOfVotesInPercent(BigDecimal.ZERO)
                                            .build()
                                    )
                                    .peek(x -> x.setNumberOfVotesInPercent(calculateAverageResult(
                                            e.getValue()
                                                    .stream()
                                                    .mapToInt(ResultDTO::getVotes_number)
                                                    .sum(),
                                            x.getNumberOfVotes()))
                                    )
                                    .sorted(Comparator.comparing(CandidateResultDTO::getNumberOfVotesInPercent, Comparator.reverseOrder()))
                                    .collect(Collectors.toList())
                    ));

        } catch (Exception e) {
            e.printStackTrace();
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    public void addVoteFirstTour(ConstituencyDTO constituencyDTO, CandidateDTO candidateDTO) {
        try {
            if (constituencyDTO == null || candidateDTO == null) {
                throw new NullPointerException("ARGS ARE NULL");
            }
            addVote(constituencyDTO, candidateDTO, ETour.FIRST_TOUR);
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    public void addVoteSecondTour(ConstituencyDTO constituencyDTO, CandidateDTO candidateDTO) {
        try {
            if (constituencyDTO == null || candidateDTO == null) {
                throw new NullPointerException("ARGS ARE NULL");
            }
            addVote(constituencyDTO, candidateDTO, ETour.SECOND_TOUR);
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    private void addVote(ConstituencyDTO constituencyDTO, CandidateDTO candidateDTO, ETour eTour) {
        this.findResultCandidatesByTour(eTour)
                .stream()
                .filter(x -> x.getCandidateDTO().getConstituencyDTO().equals(constituencyDTO))
                .filter(x -> x.getCandidateDTO().equals(candidateDTO))
                .findFirst()
                .ifPresent(x -> {
                    x.setVotes_number(x.getVotes_number() + 1);
                    this.addResult(x);
                });

    }

    public void addResult(ResultDTO resultDTO) {
        try {
            if (resultDTO == null) {
                throw new NullPointerException("RESULT IS NULL");
            }

            if (resultDTO.getCandidateDTO() == null) {
                throw new NullPointerException("CANDIDATE IS NULL");
            }

            if (resultDTO.getCandidateDTO().getId() == null) {
                throw new NullPointerException("CANDIDATE ID IS NULL");
            }

            Candidate candidate = candidateRepository
                    .findById(resultDTO.getCandidateDTO().getId())
                    .orElseThrow(NullPointerException::new);

            Result result = modelMapper.fromResultDTOToResult(resultDTO);
            result.setCandidate(candidate);
            resultRepository.saveOrUpdate(result);
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    public void deleteAll() {
        try {
            resultRepository.deleteAll();
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    public boolean isWinnerInFirstTour(String pesel) {
        try {
            if (pesel == null) {
                throw new NullPointerException("PESEL IS NULL");
            }

            VoterDTO voterDTO = voterService.findVoterByPesel(pesel)
                    .map(modelMapper::fromVoterToVoterDTO)
                    .orElseThrow(() -> new NullPointerException("VOTER IS NULL"));

            return this.findCandidatesForSecondTour()
                    .stream()
                    .map(CandidateDTO::getConstituencyDTO)
                    .distinct()
                    .noneMatch(x -> x.equals(voterDTO.getConstituencyDTO()));
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }

    public void deleteResultsByCandidate(CandidateDTO candidateDTO) {
        try {
            if (candidateDTO == null) {
                throw new NullPointerException("CANDIDATE IS NULL");
            }
            if (candidateDTO.getId() == null) {
                throw new NullPointerException("CANDIDATE ID IS NULL");
            }
            resultRepository.deleteResultsByCandidate(candidateDTO.getId());
        } catch (Exception e) {
            throw new MyException(ExceptionCode.SERVICE, e.getMessage());
        }
    }
}
