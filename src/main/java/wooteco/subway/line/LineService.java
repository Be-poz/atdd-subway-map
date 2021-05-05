package wooteco.subway.line;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LineService {
    private final LineDao lineDao;

    public LineService(LineDao lineDao) {
        this.lineDao = lineDao;
    }

    public Line createLine(String name, String color) {
        if (isStationExist(name)) {
            throw new LineExistenceException("존재하는 노선 이름입니다.");
        }
        return lineDao.save(name, color);
    }

    public List<Line> findAll() {
        return lineDao.findAll();
    }

    public Line findById(Long id) {
        return lineDao.findById(id)
                .orElseThrow(() -> new LineExistenceException("존재하지 않는 노선입니다."));
    }

    private boolean isStationExist(String name) {
        return lineDao.findByName(name)
                .isPresent();
    }

    public void modifyLine(Long id, String name, String color) {
        lineDao.update(id, name, color);
    }

    public void deleteLine(Long id) {
        lineDao.delete(id);
    }
}
