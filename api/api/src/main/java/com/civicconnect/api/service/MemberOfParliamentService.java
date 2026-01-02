package com.civicconnect.api.service;

import com.civicconnect.api.dto.MemberOfParliamentDTO;
import com.civicconnect.api.entity.MemberOfParliament;
import com.civicconnect.api.entity.ParliamentaryConstituency;
import com.civicconnect.api.repository.MemberOfParliamentRepository;
import com.civicconnect.api.repository.ParliamentaryConstituencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberOfParliamentService {

    private final MemberOfParliamentRepository repository;
    private final ParliamentaryConstituencyRepository constituencyRepository;

    public Page<MemberOfParliamentDTO> findAll(String q, String stateName, String partyName, Pageable pageable) {
        return repository.searchMembers(q, stateName, partyName, pageable)
                .map(this::toDTO);
    }

    public Optional<MemberOfParliamentDTO> findById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    public List<String> findDistinctStateNames() {
        return repository.findDistinctStateNames();
    }

    public List<String> findDistinctPartyNames() {
        return repository.findDistinctPartyNames();
    }

    public Map<String, Long> getPartyWiseCount() {
        return repository.countByParty().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    public Map<String, Long> getStateWiseCount() {
        return repository.countByState().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    public Optional<MemberOfParliamentDTO> findByConstituencyId(Long constituencyId) {
        return repository.findByConstituencyIdAndIsActiveTrue(constituencyId)
                .map(this::toDTO);
    }

    @Transactional
    public MemberOfParliamentDTO create(MemberOfParliamentDTO dto) {
        MemberOfParliament entity = toEntity(dto);
        entity.setIsActive(true);

        // Try to link to constituency
        linkToConstituency(entity, dto.getConstituencyName(), dto.getStateName());

        entity = repository.save(entity);

        // Update constituency with MP info
        updateConstituencyMpInfo(entity);

        return toDTO(entity);
    }

    @Transactional
    public MemberOfParliamentDTO update(Long id, MemberOfParliamentDTO dto) {
        MemberOfParliament entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("MP not found with id: " + id));

        entity.setMemberName(dto.getMemberName());
        entity.setPartyName(dto.getPartyName());
        entity.setPartyAbbreviation(dto.getPartyAbbreviation());
        entity.setConstituencyName(dto.getConstituencyName());
        entity.setStateName(dto.getStateName());
        entity.setMembershipStatus(dto.getMembershipStatus());
        entity.setLokSabhaTerms(dto.getLokSabhaTerms());
        entity.setCurrentTerm(dto.getCurrentTerm());
        entity.setPhotoUrl(dto.getPhotoUrl());
        entity.setGender(dto.getGender());
        entity.setEducation(dto.getEducation());
        entity.setAge(dto.getAge());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());

        // Re-link to constituency if changed
        linkToConstituency(entity, dto.getConstituencyName(), dto.getStateName());

        entity = repository.save(entity);

        // Update constituency with MP info
        updateConstituencyMpInfo(entity);

        return toDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void linkToConstituency(MemberOfParliament mp, String constituencyName, String stateName) {
        if (constituencyName != null && stateName != null) {
            // Try exact match first
            constituencyRepository.findByStateName(stateName).stream()
                    .filter(c -> normalizeConstituencyName(c.getPcName())
                            .equals(normalizeConstituencyName(constituencyName)))
                    .findFirst()
                    .ifPresent(mp::setConstituency);
        }
    }

    private void updateConstituencyMpInfo(MemberOfParliament mp) {
        if (mp.getConstituency() != null) {
            ParliamentaryConstituency pc = mp.getConstituency();
            pc.setCurrentMpName(mp.getMemberName());
            pc.setCurrentMpParty(mp.getPartyName());
            constituencyRepository.save(pc);
        }
    }

    private String normalizeConstituencyName(String name) {
        if (name == null) return "";
        // Remove (SC), (ST), normalize case and spacing
        return name.toUpperCase()
                .replaceAll("\\(SC\\)", "")
                .replaceAll("\\(ST\\)", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private MemberOfParliamentDTO toDTO(MemberOfParliament entity) {
        return MemberOfParliamentDTO.builder()
                .id(entity.getId())
                .memberName(entity.getMemberName())
                .partyName(entity.getPartyName())
                .partyAbbreviation(entity.getPartyAbbreviation())
                .constituencyName(entity.getConstituencyName())
                .stateName(entity.getStateName())
                .membershipStatus(entity.getMembershipStatus())
                .lokSabhaTerms(entity.getLokSabhaTerms())
                .currentTerm(entity.getCurrentTerm())
                .photoUrl(entity.getPhotoUrl())
                .gender(entity.getGender())
                .education(entity.getEducation())
                .age(entity.getAge())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .constituencyId(entity.getConstituency() != null ? entity.getConstituency().getId() : null)
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MemberOfParliament toEntity(MemberOfParliamentDTO dto) {
        return MemberOfParliament.builder()
                .memberName(dto.getMemberName())
                .partyName(dto.getPartyName())
                .partyAbbreviation(dto.getPartyAbbreviation())
                .constituencyName(dto.getConstituencyName())
                .stateName(dto.getStateName())
                .membershipStatus(dto.getMembershipStatus())
                .lokSabhaTerms(dto.getLokSabhaTerms())
                .currentTerm(dto.getCurrentTerm())
                .photoUrl(dto.getPhotoUrl())
                .gender(dto.getGender())
                .education(dto.getEducation())
                .age(dto.getAge())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .build();
    }

    public MemberOfParliamentDTO getByConstituencyId(Long constituencyId) {
        return repository.findByConstituencyIdAndIsActiveTrue(constituencyId)
                .map(this::toDTO)
                .orElse(null);
    }

    public List<MemberOfParliamentDTO> findByConstituencyName(String constituencyName) {
        return repository.findByConstituencyNameContainingIgnoreCaseAndIsActiveTrue(constituencyName)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
