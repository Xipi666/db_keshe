package pers.luoluo.databasekeshe.simulation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pers.luoluo.databasekeshe.simulation.dto.SimulationStatusResponse;
import pers.luoluo.databasekeshe.simulation.dto.SimulationTagProfile;
import pers.luoluo.databasekeshe.simulation.mapper.SimulationMapper;

@Service
public class SimulationService {

    private static final int NORMAL_INTERVAL_SECONDS = 60;
    private static final int BURST_INTERVAL_SECONDS = 1;
    private static final BigDecimal DEFAULT_BASE_VALUE = new BigDecimal("50.0000");
    private static final BigDecimal NORMAL_RATIO = new BigDecimal("0.72");
    private static final BigDecimal ANOMALY_RATIO = new BigDecimal("1.12");

    private final SimulationMapper simulationMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean anomalyEnabled = new AtomicBoolean(false);
    private final AtomicLong writeCount = new AtomicLong();
    private final AtomicLong alarmCount = new AtomicLong();
    private final AtomicLong taskCount = new AtomicLong();
    private final Set<String> activeRangeAlarms = ConcurrentHashMap.newKeySet();

    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime lastWriteAt;
    private volatile LocalDateTime lastNormalWriteAt;
    private volatile LocalDateTime lastBurstWriteAt;

    public SimulationService(SimulationMapper simulationMapper) {
        this.simulationMapper = simulationMapper;
    }

    public SimulationStatusResponse start() {
        if (running.compareAndSet(false, true)) {
            startedAt = LocalDateTime.now();
            lastWriteAt = null;
            lastNormalWriteAt = null;
            lastBurstWriteAt = null;
            writeCount.set(0);
            alarmCount.set(0);
            taskCount.set(0);
            activeRangeAlarms.clear();
        }
        return status();
    }

    public SimulationStatusResponse stop() {
        running.set(false);
        anomalyEnabled.set(false);
        return status();
    }

    public SimulationStatusResponse setAnomalyEnabled(boolean enabled) {
        anomalyEnabled.set(enabled);
        return status();
    }

    public SimulationStatusResponse status() {
        return new SimulationStatusResponse(
                running.get(),
                anomalyEnabled.get(),
                startedAt,
                lastWriteAt,
                writeCount.get(),
                alarmCount.get(),
                taskCount.get(),
                NORMAL_INTERVAL_SECONDS,
                BURST_INTERVAL_SECONDS
        );
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void writeTick() {
        if (!running.get()) {
            return;
        }

        LocalDateTime sampleTime = LocalDateTime.now();
        boolean abnormal = anomalyEnabled.get();
        if (!shouldWrite(sampleTime, abnormal)) {
            return;
        }

        List<SimulationTagProfile> profiles = simulationMapper.findTagProfiles();
        if (profiles.isEmpty()) {
            return;
        }

        for (SimulationTagProfile profile : profiles) {
            BigDecimal value = nextValue(profile, abnormal);
            boolean outOfRange = !inReasonableRange(profile, value);
            String alarmKey = profile.deviceId() + ":" + profile.tagId();
            simulationMapper.insertRawData(
                    profile.deviceId(),
                    profile.tagId(),
                    sampleTime,
                    value,
                    abnormal ? 1 : 0,
                    outOfRange ? 1 : 0
            );
            writeCount.incrementAndGet();

            if (outOfRange && activeRangeAlarms.add(alarmKey)) {
                createAlarmAndTask(profile, sampleTime, value, "RANGE_LIMIT");
            } else if (!outOfRange) {
                activeRangeAlarms.remove(alarmKey);
            }
        }

        if (abnormal) {
            lastBurstWriteAt = sampleTime;
        } else {
            lastNormalWriteAt = sampleTime;
        }
        lastWriteAt = sampleTime;
    }

    private boolean shouldWrite(LocalDateTime sampleTime, boolean abnormal) {
        LocalDateTime previousWriteAt = abnormal ? lastBurstWriteAt : lastNormalWriteAt;
        int interval = abnormal ? BURST_INTERVAL_SECONDS : NORMAL_INTERVAL_SECONDS;
        return previousWriteAt == null || !sampleTime.isBefore(previousWriteAt.plusSeconds(interval));
    }

    private BigDecimal nextValue(SimulationTagProfile profile, boolean abnormal) {
        BigDecimal base = profile.warnLimit() == null ? DEFAULT_BASE_VALUE : profile.warnLimit();
        BigDecimal ratio = valueRatio(profile, abnormal);
        long phase = writeCount.get() % 9;
        BigDecimal drift = new BigDecimal(phase).subtract(new BigDecimal("4")).multiply(new BigDecimal("0.1200"));
        return base.multiply(ratio).add(drift).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal valueRatio(SimulationTagProfile profile, boolean abnormal) {
        String tagCode = normalizedTagCode(profile);
        if (tagCode.contains("VOLTAGE")) {
            return abnormal ? new BigDecimal("1.08") : new BigDecimal("0.96");
        }
        return abnormal ? ANOMALY_RATIO : NORMAL_RATIO;
    }

    private boolean inReasonableRange(SimulationTagProfile profile, BigDecimal value) {
        BigDecimal min = reasonableMin(profile);
        BigDecimal max = reasonableMax(profile);
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private BigDecimal reasonableMin(SimulationTagProfile profile) {
        String tagCode = normalizedTagCode(profile);
        if (tagCode.contains("VOLTAGE")) {
            return new BigDecimal("9.5000");
        }
        if (tagCode.contains("TEMP")) {
            return new BigDecimal("-20.0000");
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal reasonableMax(SimulationTagProfile profile) {
        BigDecimal warnLimit = profile.warnLimit() == null ? DEFAULT_BASE_VALUE : profile.warnLimit();
        String tagCode = normalizedTagCode(profile);
        if (tagCode.contains("WINDING_TEMP")) {
            return warnLimit.min(new BigDecimal("120.0000"));
        }
        if (tagCode.contains("OIL_TEMP")) {
            return warnLimit.min(new BigDecimal("105.0000"));
        }
        if (tagCode.contains("AMBIENT_TEMP")) {
            return warnLimit.min(new BigDecimal("60.0000"));
        }
        if (tagCode.contains("VOLTAGE")) {
            return new BigDecimal("11.5000");
        }
        return warnLimit;
    }

    private String normalizedTagCode(SimulationTagProfile profile) {
        return profile.tagCode() == null ? "" : profile.tagCode().toUpperCase();
    }

    private void createAlarmAndTask(SimulationTagProfile profile, LocalDateTime sampleTime, BigDecimal startValue, String alarmType) {
        Long alarmId = simulationMapper.nextAlarmId();
        simulationMapper.insertAlarm(
                alarmId,
                profile.deviceId(),
                profile.tagId(),
                alarmType,
                "SERIOUS",
                sampleTime,
                startValue,
                startValue
        );
        alarmCount.incrementAndGet();

        simulationMapper.insertTask(alarmId, "模拟派单");
        taskCount.incrementAndGet();
    }
}
