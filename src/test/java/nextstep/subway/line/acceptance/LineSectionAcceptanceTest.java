package nextstep.subway.line.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static nextstep.subway.line.acceptance.LineAcceptanceTest.지하철_노선_조회_요청;
import static nextstep.subway.station.StationAcceptanceTest.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 구간 관련 기능")
public class LineSectionAcceptanceTest extends AcceptanceTest {
    private LineResponse 신분당선;
    private StationResponse 강남역;
    private StationResponse 양재역;
    private StationResponse 정자역;
    private StationResponse 광교역;

    @BeforeEach
    public void setUp() {
        super.setUp();

        강남역 = StationAcceptanceTest.지하철역_등록되어_있음("강남역").as(StationResponse.class);
        양재역 = StationAcceptanceTest.지하철역_등록되어_있음("양재역").as(StationResponse.class);
        정자역 = StationAcceptanceTest.지하철역_등록되어_있음("정자역").as(StationResponse.class);
        광교역 = StationAcceptanceTest.지하철역_등록되어_있음("광교역").as(StationResponse.class);

        LineRequest lineRequest = new LineRequest("신분당선", "bg-red-600", 강남역.getId(), 광교역.getId(), 10);
        신분당선 = LineAcceptanceTest.지하철_노선_등록되어_있음(lineRequest).as(LineResponse.class);
    }

    @DisplayName("시나리오1: 지하철 구간을 관리한다.")
    @Test
    void manageLineSectionTest() {
        StationResponse deleteTarget = 양재역;

        // when
        ExtractableResponse<Response> createResponse = 지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, 5);
        // then
        지하철_노선에_지하철역_등록됨(createResponse);

        // when
        ExtractableResponse<Response> findResponse = 지하철_노선_조회_요청(신분당선);
        // then
        지하철_노선에_지하철역_순서_정렬됨(findResponse, Arrays.asList(강남역, 양재역, 광교역));

        // when
        ExtractableResponse<Response> removeResponse = 지하철_노선에_지하철역_제외_요청(신분당선, deleteTarget);
        // then
        지하철_노선에_지하철역_제외됨(removeResponse);

        // when
        ExtractableResponse<Response> findResponseAfterDelete = 지하철_노선_조회_요청(신분당선);
        // then
        지하철_노선에_지하철역_순서_정렬됨(findResponseAfterDelete, Arrays.asList(강남역, 광교역));

        // when
        ExtractableResponse<Response> stationsResponse = 지하철역_목록_조회_요청();
        // then
        지하철_노선에서_삭제해도_역은_남아있음(stationsResponse, deleteTarget);
    }

    @DisplayName("시나리오2: 기존 지하철 노선의 종점간 거리보다 긴 종점 구간을 추가한다.")
    @Test
    void addEndLineSectionWithTooLong() {
        int tooLongDistance = 100;

        // when
        ExtractableResponse<Response> response = 지하철_노선에_상행종점역_추가_요청(신분당선, 정자역, tooLongDistance);

        // then
        지하철_노선에_지하철역_등록됨(response);
    }

    @DisplayName("시나리오3: 기존 지하철 노선의 종점가 거리보다 긴 종점이 아닌 구간을 추가한다.")
    @Test
    void addLineSectionWithTooLongTest() {
        int tooLongDistance = 100;

        // when
        ExtractableResponse<Response> response = 지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, tooLongDistance);

        // then
        지하철_노선에_지하철역_등록_실패됨(response);
    }

    @DisplayName("시나리오4: 실수로 등록되지 않은 지하철 역이 포함된 지하철 구간을 등록한다.")
    @Test
    void addLineSectionWithNotExistStationTest() {
        StationResponse 없는역 = new StationResponse(100L, "없는역", LocalDateTime.now(), null);

        // when
        ExtractableResponse<Response> response = 지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 없는역, 1);

        // then
        지하철_노선에_지하철역_등록_실패됨(response);
    }

    @DisplayName("시나리오5: 실수로 기존 지하철 노선과 접점이 없는 지하철 구간을 등록한다.")
    @Test
    void addLineSectionWithoutDuplicatedStationTest() {
        StationResponse upStation = 양재역;
        StationResponse downStation = 정자역;
        // given
        ExtractableResponse<Response> findResponse = 지하철_노선_조회_요청(신분당선);
        지하철_노선에_지하철역_포함안됨(findResponse, Arrays.asList(upStation, downStation));

        // when
        ExtractableResponse<Response> response = 지하철_노선에_지하철역_등록_요청(신분당선, upStation, downStation, 1);

        // then
        지하철_노선에_지하철역_등록_실패됨(response);
    }

    @DisplayName("시나리오6: 실수로 똑같은 지하철 구간을 두번 등록한다.")
    @Test
    void addLineSectionTwiceTest() {
        // given
        ExtractableResponse<Response> firstResponse = 지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, 1);
        지하철_노선에_지하철역_등록됨(firstResponse);

        // when
        ExtractableResponse<Response> secondResponse = 지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, 1);

        // then
        지하철_노선에_지하철역_등록_실패됨(secondResponse);
    }

    @DisplayName("시나리오7: 하나밖에 안남은 지하철 구간의 역을 삭제한다.")
    @Test
    void tryDeleteWhenLineHasJustOneSectionTest() {
        // given
        지하철_노선에_구간_하나밖에_없음(신분당선);

        // when
        ExtractableResponse<Response> response = 지하철_노선에_지하철역_제외_요청(신분당선, 강남역);

        // then
        지하철_노선에_지하철역_등록_실패됨(response);
    }

    public static ExtractableResponse<Response> 지하철_노선에_지하철역_등록_요청(LineResponse line, StationResponse upStation, StationResponse downStation, int distance) {
        SectionRequest sectionRequest = new SectionRequest(upStation.getId(), downStation.getId(), distance);

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(sectionRequest)
                .when().post("/lines/{lineId}/sections", line.getId())
                .then().log().all()
                .extract();
    }

    public static void 지하철_노선에_지하철역_등록되어_있음(LineResponse line, StationResponse upStation, StationResponse downStation, int distance) {
        ExtractableResponse<Response> response = 지하철_노선에_지하철역_등록_요청(line, upStation, downStation, distance);
        지하철_노선에_지하철역_등록됨(response);
    }

    public static void 지하철_노선에_지하철역_등록됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 지하철_노선에_지하철역_등록_실패됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    public static void 지하철_노선에_지하철역_순서_정렬됨(ExtractableResponse<Response> response, List<StationResponse> expectedStations) {
        LineResponse line = response.as(LineResponse.class);
        List<Long> stationIds = line.getStations().stream()
                .map(it -> it.getId())
                .collect(Collectors.toList());

        List<Long> expectedStationIds = expectedStations.stream()
                .map(it -> it.getId())
                .collect(Collectors.toList());

        assertThat(stationIds).containsExactlyElementsOf(expectedStationIds);
    }

    public static void 지하철_노선에_지하철역_포함안됨(ExtractableResponse<Response> response, List<StationResponse> expectedStations) {
        LineResponse line = response.as(LineResponse.class);
        List<Long> stationIds = line.getStations().stream()
                .map(it -> it.getId())
                .collect(Collectors.toList());

        List<Long> expectedStationIds = expectedStations.stream()
                .map(it -> it.getId())
                .collect(Collectors.toList());

        assertThat(stationIds).doesNotContainAnyElementsOf(expectedStationIds);
    }

    public static ExtractableResponse<Response> 지하철_노선에_지하철역_제외_요청(LineResponse line, StationResponse station) {
        return RestAssured
                .given().log().all()
                .when().delete("/lines/{lineId}/sections?stationId={stationId}", line.getId(), station.getId())
                .then().log().all()
                .extract();
    }

    public static void 지하철_노선에_지하철역_제외됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 지하철_노선에_지하철역_제외_실패됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public static ExtractableResponse<Response> 지하철_노선에_상행종점역_추가_요청(
            LineResponse lineResponse, StationResponse newUpEndStation, int distance
    ) {
        ExtractableResponse<Response> findResponse = 지하철_노선_조회_요청(lineResponse);
        LineResponse line = findResponse.as(LineResponse.class);
        StationResponse upEndStation = line.getStations().get(0);

        return 지하철_노선에_지하철역_등록_요청(lineResponse, newUpEndStation, upEndStation, distance);
    }

    public static void 지하철_노선에_구간_하나밖에_없음(LineResponse line) {
        ExtractableResponse<Response> response = 지하철_노선_조회_요청(line);
        LineResponse lineResponse = response.as(LineResponse.class);
        assertThat(lineResponse.getStations()).hasSize(2);
    }
}
