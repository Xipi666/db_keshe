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
import pers.luoluo.databasekeshe.simulation.dto.SimulationPointProfile;
import pers.luoluo.databasekeshe.simulation.dto.SimulationStatusResponse;
import pers.luoluo.databasekeshe.simulation.mapper.SimulationMapper;

@Service
public class SimulationService {

    private static final int SAMPLE_INTERVAL_SECONDS = 1;

    private final SimulationMapper simulationMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean anomalyEnabled = new AtomicBoolean(false);
    private final AtomicLong writeCount = new AtomicLong();
    private final AtomicLong alarmCount = new AtomicLong();
    private final AtomicLong taskCount = new AtomicLong();
    private final Set<String> activeRangeAlarms = ConcurrentHashMap.newKeySet();

    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime lastWriteAt;

    public SimulationService(SimulationMapper simulationMapper) {
        this.simulationMapper = simulationMapper;
    }

    public SimulationStatusResponse start() {
        if (running.compareAndSet(false, true)) {
            startedAt = LocalDateTime.now();
            lastWriteAt = null;
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
                SAMPLE_INTERVAL_SECONDS
        );
    }

    @Scheduled(fixedDelay = 1000)
    public void writeTick() {
        if (!running.get()) {
            return;
        }

        LocalDateTime sampleTime = LocalDateTime.now();
        if (!shouldWrite(sampleTime)) {
            return;
        }

        List<SimulationPointProfile> profiles = simulationMapper.findPointProfiles();
        if (profiles.isEmpty()) {
            return;
        }

        boolean abnormal = anomalyEnabled.get();
        for (SimulationPointProfile profile : profiles) {
            BigDecimal value = nextValue(profile, abnormal);
            boolean outOfRange = !inReasonableRange(profile, value);
            String alarmKey = profile.transformerId() + ":" + profile.pointId();
            simulationMapper.insertRawData(
                    profile.transformerId(),
                    profile.circuitId(),
                    profile.pointId(),
                    sampleTime,
                    value,
                    outOfRange ? 1 : 0
            );
            writeCount.incrementAndGet();

            if (outOfRange && activeRangeAlarms.add(alarmKey)) {
                createAlarmAndTask(profile, sampleTime, value, alarmType(profile, value));
            } else if (!outOfRange) {
                activeRangeAlarms.remove(alarmKey);
            }
        }

        lastWriteAt = sampleTime;
    }

    private boolean shouldWrite(LocalDateTime sampleTime) {
        return lastWriteAt == null || !sampleTime.isBefore(lastWriteAt.plusSeconds(SAMPLE_INTERVAL_SECONDS));
    }

    private BigDecimal nextValue(SimulationPointProfile profile, boolean abnormal) {
        BigDecimal base = baseValue(profile, abnormal);
        String type = normalizedType(profile);
        if (type.endsWith("_STATUS") || "POWER_FACTOR".equals(type)) {
            return base.setScale(4, RoundingMode.HALF_UP);
        }
        long phase = writeCount.get() % 7;
        BigDecimal drift = new BigDecimal(phase).subtract(new BigDecimal("3")).multiply(new BigDecimal("0.0500"));
        return base.add(drift).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal baseValue(SimulationPointProfile profile, boolean abnormal) {
        String type = normalizedType(profile);
        return switch (type) {
            case "VOLTAGE" -> voltageValue(profile, abnormal);
            case "CURRENT" -> currentValue(profile, abnormal);
            case "POWER_FACTOR" -> abnormal ? new BigDecimal("0.7800") : new BigDecimal("0.9300");
            case "ENERGY" -> new BigDecimal("12000.0000").add(BigDecimal.valueOf(writeCount.get()).multiply(new BigDecimal("0.0200")));
            case "FREQUENCY" -> abnormal ? new BigDecimal("49.2000") : new BigDecimal("50.0200");
            case "OIL_TEMP" -> abnormal ? new BigDecimal("91.0000") : new BigDecimal("63.0000");
            case "CABINET_TEMP" -> abnormal ? new BigDecimal("50.0000") : new BigDecimal("32.0000");
            case "CABINET_HUMIDITY" -> abnormal ? new BigDecimal("88.0000") : new BigDecimal("55.0000");
            case "SWITCH_STATUS" -> BigDecimal.ONE;
            case "FUSE_STATUS", "SMOKE_STATUS" -> abnormal ? BigDecimal.ONE : BigDecimal.ZERO;
            case "DOOR_STATUS" -> abnormal ? BigDecimal.ONE : BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal voltageValue(SimulationPointProfile profile, boolean abnormal) {
        String unit = profile.unit() == null ? "" : profile.unit().toUpperCase();
        if ("V".equals(unit)) {
            return abnormal ? new BigDecimal("445.0000") : new BigDecimal("400.0000");
        }
        return abnormal ? new BigDecimal("11.2000") : new BigDecimal("10.1000");
    }

    private BigDecimal currentValue(SimulationPointProfile profile, boolean abnormal) {
        BigDecimal max = profile.maxLimit() == null ? new BigDecimal("100.0000") : profile.maxLimit();
        return max.multiply(abnormal ? new BigDecimal("1.0800") : new BigDecimal("0.6200"));
    }

    private boolean inReasonableRange(SimulationPointProfile profile, BigDecimal value) {
        if (profile.minLimit() != null && value.compareTo(profile.minLimit()) < 0) {
            return false;
        }
        return profile.maxLimit() == null || value.compareTo(profile.maxLimit()) <= 0;
    }

    private String alarmType(SimulationPointProfile profile, BigDecimal value) {
        if ("POWER_FACTOR".equals(normalizedType(profile)) && profile.minLimit() != null && value.compareTo(profile.minLimit()) < 0) {
            return "POWER_FACTOR_LOW";
        }
        return "RANGE_LIMIT";
    }

    private String normalizedType(SimulationPointProfile profile) {
        return profile.measureType() == null ? "" : profile.measureType().toUpperCase();
    }

    private void createAlarmAndTask(SimulationPointProfile profile, LocalDateTime sampleTime, BigDecimal startValue, String alarmType) {
        Long alarmId = simulationMapper.nextAlarmId();
        simulationMapper.insertAlarm(
                alarmId,
                profile.transformerId(),
                profile.circuitId(),
                profile.pointId(),
                alarmType,
                "SERIOUS",
                sampleTime,
                startValue,
                startValue
        );
        alarmCount.incrementAndGet();

        simulationMapper.insertTask(alarmId, "engineer01");
        taskCount.incrementAndGet();
    }
}
