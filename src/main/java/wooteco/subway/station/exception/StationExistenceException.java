package wooteco.subway.station.exception;

public class StationExistenceException extends StationException {
    private static final String message = "존재하는 역 이름입니다.";

    public StationExistenceException() {
        super(message);
    }
}
