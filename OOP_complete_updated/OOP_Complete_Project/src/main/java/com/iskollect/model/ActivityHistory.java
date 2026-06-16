package com.iskollect.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivityHistory {
    private final List<Object> entries;

    public ActivityHistory(List<Object> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<Object> getEntries() {
        return entries;
    }

    public List<BottleRecord> getSubmissions() {
        List<BottleRecord> submissions = new ArrayList<>();
        for (Object entry : entries) {
            if (entry instanceof BottleRecord) {
                submissions.add((BottleRecord) entry);
            }
        }
        return submissions;
    }

    public List<Redemption> getRedemptions() {
        List<Redemption> redemptions = new ArrayList<>();
        for (Object entry : entries) {
            if (entry instanceof Redemption) {
                redemptions.add((Redemption) entry);
            }
        }
        return redemptions;
    }
}
