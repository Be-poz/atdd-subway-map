package wooteco.subway.section;

import org.springframework.stereotype.Service;
import wooteco.subway.section.exception.SectionDistanceException;
import wooteco.subway.section.exception.SectionInclusionException;
import wooteco.subway.section.exception.SectionInitializationException;
import wooteco.subway.section.exception.SectionNotFoundException;
import wooteco.subway.station.exception.StationNotFoundException;

@Service
public class SectionService {
    private SectionDao sectionDao;

    public SectionService(SectionDao sectionDao) {
        this.sectionDao = sectionDao;
    }

    public Section initialize(SectionDto sectionDto) {
        return sectionDao.save(sectionDto.getLineId(), sectionDto.getUpStationId(),
                sectionDto.getDownStationId(), sectionDto.getDistance());
    }

    public Section save(SectionDto sectionDto) {
        validateSectionInitialization(sectionDto.getLineId());
        validateSectionInclusion(sectionDto);
        if (hasEndStationInSection(sectionDto)) {
            return sectionDao.save(sectionDto.getLineId(), sectionDto.getUpStationId(),
                    sectionDto.getDownStationId(), sectionDto.getDistance());
        }
        return saveWithForkCase(sectionDto);
    }

    public void delete(Long lineId, Long stationId) {
        validateSectionInitialization(lineId);
        if (sectionDao.isExistingStation(lineId, stationId)) {
            throw new StationNotFoundException();
        }
        if (sectionDao.isEndStation(lineId, stationId)) {
            sectionDao.delete(lineId, stationId);
        }
        deleteAndConnectSection(lineId, stationId);
    }

    private void deleteAndConnectSection(Long lineId, Long stationId) {
        Section frontSection = sectionDao.findSectionByDownStationId(lineId, stationId)
                .orElseThrow(SectionNotFoundException::new);
        Section backSection = sectionDao.findSectionByUpStationId(lineId, stationId)
                .orElseThrow(SectionNotFoundException::new);

        sectionDao.updateDistanceAndDownStation(
                lineId, frontSection.getUpStationId(), backSection.getDownStationId(),
                frontSection.getDistance() + backSection.getDistance()
        );
        sectionDao.delete(backSection.getId());
    }

    private void validateSectionInitialization(Long lineId) {
        if (!sectionDao.isExistingLine(lineId)) {
            throw new SectionInitializationException();
        }
    }

    private void validateSectionInclusion(SectionDto sectionDto) {
        if (hasBothStations(sectionDto.getLineId(), sectionDto.getUpStationId(), sectionDto.getDownStationId()) ||
                hasNeitherStations(sectionDto.getLineId(), sectionDto.getUpStationId(), sectionDto.getDownStationId())) {
            throw new SectionInclusionException();
        }
    }

    private boolean hasBothStations(Long lineId, Long upStationId, Long downStationId) {
        return sectionDao.isExistingStation(lineId, upStationId) &&
                sectionDao.isExistingStation(lineId, downStationId);
    }

    private boolean hasNeitherStations(Long lineId, Long upStationId, Long downStationId) {
        return !sectionDao.isExistingStation(lineId, upStationId) &&
                !sectionDao.isExistingStation(lineId, downStationId);
    }

    private boolean hasEndStationInSection(SectionDto sectionDto) {
        return sectionDao.hasEndStationInSection(sectionDto.getLineId(),
                sectionDto.getUpStationId(), sectionDto.getDownStationId());
    }

    private Section saveWithForkCase(SectionDto sectionDto) {
        Section findSection = findSectionWithExistingStation(sectionDto);
        validateSectionDistance(sectionDto, findSection);

        sectionDao.delete(findSection.getId());
        Section savedSection = sectionDao.save(sectionDto.getLineId(), sectionDto.getUpStationId(),
                sectionDto.getDownStationId(), sectionDto.getDistance());
        sectionDao.save(sectionDto.getLineId(), sectionDto.getDownStationId(),
                findSection.getDownStationId(), findSection.getDistance() - sectionDto.getDistance());
        return savedSection;
    }

    private Section findSectionWithExistingStation(SectionDto sectionDto) {
        SectionStandard sectionStandard = calculateSectionStandard(sectionDto);
        if (sectionStandard == SectionStandard.FROM_UP_STATION) {
             return sectionDao.findSectionByUpStationId(sectionDto.getLineId(), sectionDto.getUpStationId())
                     .orElseThrow(SectionNotFoundException::new);
        }
        return sectionDao.findSectionByDownStationId(sectionDto.getLineId(), sectionDto.getDownStationId())
                .orElseThrow(SectionNotFoundException::new);
    }

    private void validateSectionDistance(SectionDto sectionDto, Section findSection) {
        if (sectionDto.getDistance() >= findSection.getDistance()) {
            throw new SectionDistanceException();
        }
    }

    private SectionStandard calculateSectionStandard(SectionDto sectionDto) {
        if (sectionDao.isExistingStation(sectionDto.getLineId(), sectionDto.getUpStationId())) {
            return SectionStandard.FROM_UP_STATION;
        }
        return SectionStandard.FROM_DOWN_STATION;
    }
}
