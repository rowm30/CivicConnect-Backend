package com.civicconnect.api.service;

import com.civicconnect.api.dto.StateDTO;
import com.civicconnect.api.dto.StateMapper;
import com.civicconnect.api.entity.State;
import com.civicconnect.api.exception.DuplicateResourceException;
import com.civicconnect.api.exception.ResourceNotFoundException;
import com.civicconnect.api.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StateService {

    private final StateRepository stateRepository;
    private final StateMapper stateMapper;

    public Page<StateDTO> getAllStates(int page, int size, String sortBy, String sortDir, String search) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<State> states;
        if (search != null && !search.trim().isEmpty()) {
            states = stateRepository.search(search.trim(), pageable);
        } else {
            states = stateRepository.findAll(pageable);
        }

        return states.map(stateMapper::toDTO);
    }

    public List<StateDTO> getAllActiveStates() {
        return stateRepository.findByIsActiveTrue()
                .stream()
                .map(stateMapper::toDTO)
                .collect(Collectors.toList());
    }

    public StateDTO getStateById(Long id) {
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State", id));
        return stateMapper.toDTO(state);
    }

    public StateDTO getStateByCode(String code) {
        State state = stateRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("State not found with code: " + code));
        return stateMapper.toDTO(state);
    }

    public StateDTO createState(StateDTO dto) {
        // Check for duplicate code
        if (stateRepository.existsByCode(dto.getCode().toUpperCase())) {
            throw new DuplicateResourceException("State with code '" + dto.getCode() + "' already exists");
        }

        State state = stateMapper.toEntity(dto);
        state.setCode(state.getCode().toUpperCase());
        state.setIsActive(true);

        State saved = stateRepository.save(state);
        return stateMapper.toDTO(saved);
    }

    public StateDTO updateState(Long id, StateDTO dto) {
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State", id));

        // Check if code is being changed and if new code exists
        if (dto.getCode() != null && !dto.getCode().equalsIgnoreCase(state.getCode())) {
            if (stateRepository.existsByCode(dto.getCode().toUpperCase())) {
                throw new DuplicateResourceException("State with code '" + dto.getCode() + "' already exists");
            }
        }

        stateMapper.updateEntity(state, dto);
        if (state.getCode() != null) {
            state.setCode(state.getCode().toUpperCase());
        }

        State updated = stateRepository.save(state);
        return stateMapper.toDTO(updated);
    }

    public void deleteState(Long id) {
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("State", id));

        // Soft delete - just mark as inactive
        state.setIsActive(false);
        stateRepository.save(state);
    }

    public void hardDeleteState(Long id) {
        if (!stateRepository.existsById(id)) {
            throw new ResourceNotFoundException("State", id);
        }
        stateRepository.deleteById(id);
    }

    public List<StateDTO> getStatesByType(State.StateType stateType) {
        return stateRepository.findByStateType(stateType)
                .stream()
                .map(stateMapper::toDTO)
                .collect(Collectors.toList());
    }
}