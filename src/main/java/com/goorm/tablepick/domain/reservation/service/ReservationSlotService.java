package com.goorm.tablepick.domain.reservation.service;

import com.goorm.tablepick.domain.reservation.entity.ReservationSlot;
import com.goorm.tablepick.domain.reservation.repository.ReservationSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReservationSlotService {

    private final ReservationSlotGenerator slotGenerator;
    private final ReservationSlotRepository slotRepository;

    @Transactional
    public void bulkInsert(List<ReservationSlot> reservationSlots) {
        slotRepository.saveAll(reservationSlots);
    }

    @Transactional
    public void bulkDelete() {
        slotRepository.deleteAll();
    }
}