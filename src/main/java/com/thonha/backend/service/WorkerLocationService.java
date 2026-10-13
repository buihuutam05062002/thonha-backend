package com.thonha.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerLocationService {

    private static final String GEO_KEY = "worker:geo";
    private static final String LAST_UPDATE_KEY = "worker:last_update";

    public static final long STALE_MILLIS = 3 * 60 * 1000L;

    private static final String TRACKING_PREFIX = "tracking:request:";
    private static final Duration TRACKING_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;

    public void saveTracking(Long requestId, double lat, double lng) {
        try {
            redis.opsForValue().set(TRACKING_PREFIX + requestId,
                    lat + "," + lng + "," + System.currentTimeMillis(), TRACKING_TTL);
        } catch (RuntimeException e) {
            log.warn("Không lưu được vị trí theo dõi của yêu cầu {}: {}", requestId, e.getMessage());
        }
    }

    public Optional<TrackedLocation> getTracking(Long requestId) {
        try {
            String v = redis.opsForValue().get(TRACKING_PREFIX + requestId);
            if (v == null) return Optional.empty();
            String[] parts = v.split(",");
            return Optional.of(new TrackedLocation(
                    Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Long.parseLong(parts[2])));
        } catch (RuntimeException e) {
            log.warn("Không đọc được vị trí theo dõi của yêu cầu {}: {}", requestId, e.getMessage());
            return Optional.empty();
        }
    }

    public void clearTracking(Long requestId) {
        try {
            redis.delete(TRACKING_PREFIX + requestId);
        } catch (RuntimeException e) {
            log.warn("Không xóa được vị trí theo dõi của yêu cầu {}: {}", requestId, e.getMessage());
        }
    }

    public record TrackedLocation(double lat, double lng, long updatedAt) {
    }

    /** Ghi vị trí mới của thợ (gọi mỗi ~20 giây từ dashboard thợ). */
    public void update(Long workerProfileId, double lat, double lng) {
        String member = String.valueOf(workerProfileId);
        redis.opsForGeo().add(GEO_KEY, new Point(lng, lat), member);
        redis.opsForHash().put(LAST_UPDATE_KEY, member, String.valueOf(System.currentTimeMillis()));
    }

    /** Xóa vị trí khi thợ tắt sẵn sàng / đăng xuất. */
    public void remove(Long workerProfileId) {
        String member = String.valueOf(workerProfileId);
        redis.opsForGeo().remove(GEO_KEY, member);
        redis.opsForHash().delete(LAST_UPDATE_KEY, member);
    }

    public List<NearbyWorker> findNearby(double lat, double lng, double radiusKm, int limit) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().radius(
                GEO_KEY,
                new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeCoordinates()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit * 3L)); // lấy dư vì còn loại các vị trí đã cũ
        if (results == null) return List.of();

        long now = System.currentTimeMillis();
        List<NearbyWorker> out = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> r : results.getContent()) {
            String member = r.getContent().getName();
            Object last = redis.opsForHash().get(LAST_UPDATE_KEY, member);
            if (last == null || now - Long.parseLong(last.toString()) > STALE_MILLIS) {
                continue; // vị trí cũ: thợ có thể đã đóng tab / mất mạng
            }
            Point p = r.getContent().getPoint();
            out.add(new NearbyWorker(Long.valueOf(member), p.getY(), p.getX(), r.getDistance().getValue()));
            if (out.size() >= limit) break;
        }
        return out;
    }

    public record NearbyWorker(Long workerProfileId, double lat, double lng, double distanceKm) {
    }
}
