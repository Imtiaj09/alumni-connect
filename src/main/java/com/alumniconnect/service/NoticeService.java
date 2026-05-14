package com.alumniconnect.service;

import com.alumniconnect.entity.Notice;
import com.alumniconnect.repository.NoticeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public List<Notice> getNoticesByBatch(Long batchId) {
        return noticeRepository.findByBatchIdOrderByPinnedDescCreatedAtDesc(batchId);
    }

    public Notice save(Notice notice) {
        return noticeRepository.save(notice);
    }

    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }
}
