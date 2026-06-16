package com.iskollect.service;

import com.iskollect.dao.RedemptionDAO;
import com.iskollect.dao.BottleRecordDAO;
import com.iskollect.exception.DatabaseException;
import com.iskollect.model.Redemption;
import com.iskollect.model.BottleRecord;
import com.iskollect.model.ActivityHistory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActivityHistoryService {
    public enum HistoryFilter {
        TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR
    }

    private final BottleRecordDAO bottleRecordDAO = new BottleRecordDAO();
    private final RedemptionDAO redemptionDAO = new RedemptionDAO();

    public ActivityHistory getFullHistory(int userId) {
        try {
            List<Object> entries = new ArrayList<>();
            entries.addAll(bottleRecordDAO.getByUserId(userId));
            entries.addAll(redemptionDAO.getByUserId(userId));
            entries.sort(Comparator.comparing(this::entryDate).reversed());
            return new ActivityHistory(entries);
        } catch (DatabaseException e) {
            return new ActivityHistory(List.of());
        }
    }

    public ActivityHistory getFilteredHistory(int userId, HistoryFilter filter) {
        LocalDate today = LocalDate.now();
        LocalDate from;
        if (filter == HistoryFilter.TODAY) {
            from = today;
        } else if (filter == HistoryFilter.THIS_WEEK) {
            from = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        } else if (filter == HistoryFilter.THIS_MONTH) {
            from = today.withDayOfMonth(1);
        } else {
            from = today.withDayOfYear(1);
        }

        List<Object> filtered = new ArrayList<>();
        for (Object entry : getFullHistory(userId).getEntries()) {
            LocalDate date = entryDate(entry);
            if (!date.isBefore(from) && !date.isAfter(today)) {
                filtered.add(entry);
            }
        }
        return new ActivityHistory(filtered);
    }

    private LocalDate entryDate(Object entry) {
        if (entry instanceof BottleRecord) {
            return ((BottleRecord) entry).getDate();
        }
        return ((Redemption) entry).getRedemptionDate();
    }
}
