package com.pac.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class GatePendingService {
    private final AtomicBoolean pending = new AtomicBoolean(false);

    public void request() {
        pending.set(true);
    }

    public boolean consume() {
        return pending.getAndSet(false);
    }
}
