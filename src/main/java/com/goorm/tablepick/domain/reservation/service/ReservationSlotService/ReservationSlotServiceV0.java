package com.goorm.tablepick.domain.reservation.service.ReservationSlotService;

import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReservationSlotServiceV0 {
    private final ReservationSlotRepository slotRepository;

    @Transactional
    public void bulkInsert(List<ReservationSlot> slots) {
        slotRepository.saveAll(slots);
    }

    @Transactional
    public void bulkDelete() {
        slotRepository.deleteAll();
    }
}