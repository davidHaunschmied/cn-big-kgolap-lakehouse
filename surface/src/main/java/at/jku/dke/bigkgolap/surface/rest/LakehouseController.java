package at.jku.dke.bigkgolap.surface.rest;

import at.jku.dke.bigkgolap.api.engines.Engine;
import at.jku.dke.bigkgolap.api.model.CubeSchema;
import at.jku.dke.bigkgolap.shared.cache.GraphCache;
import at.jku.dke.bigkgolap.shared.context.DatabaseService;
import at.jku.dke.bigkgolap.shared.ingestionlog.FileIngestionLog;
import at.jku.dke.bigkgolap.shared.ingestionlog.FileIngestionLoggingRepository;
import at.jku.dke.bigkgolap.shared.messaging.MessagingService;
import at.jku.dke.bigkgolap.shared.querylog.QueryLog;
import at.jku.dke.bigkgolap.shared.querylog.QueryLoggingRepository;
import at.jku.dke.bigkgolap.shared.storage.StorageService;
import at.jku.dke.bigkgolap.surface.query.QueryService;
import at.jku.dke.bigkgolap.surface.rest.dto.LakehouseStatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class LakehouseController {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");

    private final Map<String, Engine> engines;

    private final StorageService storageService;

    private final DatabaseService databaseService;

    private final QueryService queryService;

    private final QueryLoggingRepository queryLogging;

    private final FileIngestionLoggingRepository ingestionLogging;

    private final MessagingService messagingService;

    private final GraphCache graphCache;

    public LakehouseController(Map<String, Engine> engines, StorageService storageService, DatabaseService databaseService, QueryService queryService, QueryLoggingRepository queryLogging, FileIngestionLoggingRepository ingestionLogging, MessagingService messagingService, GraphCache graphCache) {
        this.engines = engines;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.queryService = queryService;
        this.queryLogging = queryLogging;
        this.ingestionLogging = ingestionLogging;
        this.messagingService = messagingService;
        this.graphCache = graphCache;
    }

    @PostMapping
    public ResponseEntity<String> ingestFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        log.info("Ingesting new file {}", file.getOriginalFilename());
        final String storedName = prefix() + "_" + file.getOriginalFilename();
        final String typeParam = request.getParameter("type");

        if (typeParam == null) {
            return ResponseEntity.badRequest().body("Type parameter is not set!");
        }

        final String fileType = typeParam.toUpperCase();

        if (!engines.containsKey(fileType)) {
            return ResponseEntity.badRequest().body("Invalid type given. No engine for type '" + fileType + "' found!");
        }

        this.ingestionLogging.ingestionStarted(storedName);
        this.storageService.store(file, storedName);

        if (!databaseService.upsertFileDetails(storedName, fileType, file.getOriginalFilename(), file.getSize())) {
            return ResponseEntity.internalServerError().body("Could not ingest file!");
        }

        messagingService.registerNewFile(storedName, fileType);

        return ResponseEntity.ok(storedName);
    }

    private String prefix() {
        return FORMAT.format(new Date());
    }

    @ResponseBody
    @PostMapping("/cube")
    public void getCube(@RequestBody CubeRequest cubeRequest, HttpServletResponse response) throws IOException {
        final String requestUuid = UUID.randomUUID().toString();
        response.setHeader("request-uuid", requestUuid);

        CubeResult cubeResult = queryQube(cubeRequest, requestUuid);

        try (ServletOutputStream stream = response.getOutputStream()) {
            for (String quad : cubeResult.getQuads()) {
                stream.print(quad);
            }
        }
    }

    private CubeResult queryQube(CubeRequest cubeRequest, String requestUuid) {
        this.queryLogging.registerQueryStart(requestUuid, cubeRequest.getQuery());

        CubeResult cubeResult;
        try {
            cubeResult = queryService.getCube(cubeRequest.getQuery(), requestUuid);
        } catch (RuntimeException e) {
            this.queryLogging.registerQueryFailed(requestUuid, e.getMessage());
            throw e;
        }


        if (!cubeResult.isSuccess()) {
            this.queryLogging.registerQueryFailed(requestUuid, "One or more contexts could not be queried!",
                    cubeResult.getTs1QueryRelevant(), cubeResult.getTs2QueryGeneral(), cubeResult.getTs3PrepareQuery());
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not build the entire cube. Check the query log for details.");
        } else {
            this.queryLogging.registerQuerySucceeded(requestUuid, cubeResult.getConsideredContexts(),
                    cubeResult.getNrOfContexts(), cubeResult.getNrOfQuads(), cubeResult.getTs1QueryRelevant(),
                    cubeResult.getTs2QueryGeneral(), cubeResult.getTs3PrepareQuery());
        }

        return cubeResult;
    }

    @ResponseBody
    @PostMapping("/test/cube")
    public ResponseEntity<Long> testGetCubePerformance(@RequestBody CubeRequest cubeRequest, HttpServletResponse response) throws IOException {
        final String requestUuid = UUID.randomUUID().toString();
        response.setHeader("request-uuid", requestUuid);
        long start = System.currentTimeMillis();

        queryQube(cubeRequest, requestUuid);

        return ResponseEntity.ok(System.currentTimeMillis() - start);
    }

    @PutMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        return ResponseEntity.ok(graphCache.clear());
    }

    @GetMapping("/stats")
    public LakehouseStatsDto getLakehouseStats() {
        return LakehouseStatsDto.from(databaseService.getLakehouseStats());
    }

    @GetMapping("/querylogs")
    public List<QueryLog> getQueryLogs() {
        return this.queryLogging.getLogs();
    }

    @GetMapping("/ingestionlogs")
    public List<FileIngestionLog> getFileIngestionLogs() {
        return this.ingestionLogging.getLogs();
    }

    @GetMapping("/levels")
    public Map<String, List<LevelDto>> getLevels() {
        Map<String, List<LevelDto>> map = new HashMap<>(CubeSchema.getInstance().getDimensions().size());
        for (String dimension : CubeSchema.getInstance().getDimensions()) {
            map.put(dimension, CubeSchema.getInstance().byDimension(dimension).stream().map(LevelDto::new).collect(Collectors.toList()));
        }
        return map;
    }

    @GetMapping("/validateLogin")
    public Boolean validateLogin() {
        return true;
    }
}
