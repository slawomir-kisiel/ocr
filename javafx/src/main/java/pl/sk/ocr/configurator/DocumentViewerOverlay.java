package pl.sk.ocr.configurator;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import pl.sk.ocr.config.dto.RegionDto;
import pl.sk.ocr.configurator.viewer.ScaledCoordinateMapper;
import pl.sk.ocr.configurator.viewer.ViewerPoint;

final class DocumentViewerOverlay extends javafx.scene.layout.Pane {
    private static final double REGION_HIT_TOLERANCE = 6.0;
    private static final double MIN_REGION_SIZE = 1.0;

    private final ImageView imageView;
    private final RegionUpdateHandler updateDraftRegion;
    private final java.util.function.Consumer<RegionDto> applyDrawnRegion;
    private ScaledCoordinateMapper mapper = new ScaledCoordinateMapper(1, 0, 0);
    private ConfiguratorApplication.ViewerMode mode = ConfiguratorApplication.ViewerMode.SELECT;
    private ViewerPoint dragStart;
    private Rectangle draftRegion;
    private final List<EditableRegion> editableRegions = new ArrayList<>();
    private EditableRegion activeEditableRegion;
    private RegionDragMode regionDragMode = RegionDragMode.NONE;
    private double regionDragStartX;
    private double regionDragStartY;
    private double regionStartX;
    private double regionStartY;
    private double regionStartWidth;
    private double regionStartHeight;

    DocumentViewerOverlay(ImageView imageView, RegionUpdateHandler updateDraftRegion,
                          java.util.function.Consumer<RegionDto> applyDrawnRegion) {
        this.imageView = imageView;
        this.updateDraftRegion = updateDraftRegion;
        this.applyDrawnRegion = applyDrawnRegion;
        getChildren().add(imageView);
        setPadding(new Insets(12));
        imageView.setPreserveRatio(true);
        configureMouseHandlers();
    }

    Insets padding() {
        return getPadding();
    }

    void setContentSize(double width, double height) {
        var padding = getPadding();
        setMinSize(width + padding.getLeft() + padding.getRight(), height + padding.getTop() + padding.getBottom());
        setPrefSize(width + padding.getLeft() + padding.getRight(), height + padding.getTop() + padding.getBottom());
    }

    ScaledCoordinateMapper mapper() {
        return mapper;
    }

    void setMapper(ScaledCoordinateMapper mapper) {
        this.mapper = mapper;
    }

    void mode(ConfiguratorApplication.ViewerMode mode) {
        this.mode = mode;
        regionDragMode = RegionDragMode.NONE;
        setCursor(cursorForMode(mode));
    }

    void addOverlay(Rectangle rectangle) {
        getChildren().add(rectangle);
    }

    void editableRegion(Rectangle rectangle, RegionTargetType type) {
        editableRegions.add(new EditableRegion(rectangle, type));
    }

    void clearEditableRegions() {
        editableRegions.clear();
        activeEditableRegion = null;
    }

    void clearOverlay() {
        getChildren().removeIf(node -> node != imageView);
        draftRegion = null;
        clearEditableRegions();
        regionDragMode = RegionDragMode.NONE;
    }

    private void configureMouseHandlers() {
        setOnMousePressed(event -> {
            if (mode == ConfiguratorApplication.ViewerMode.SELECT) {
                regionDragMode = hitEditableRegion(event.getX(), event.getY()).mode();
                if (regionDragMode != RegionDragMode.NONE) {
                    regionDragStartX = event.getX();
                    regionDragStartY = event.getY();
                    regionStartX = activeEditableRegion.rectangle().getX();
                    regionStartY = activeEditableRegion.rectangle().getY();
                    regionStartWidth = activeEditableRegion.rectangle().getWidth();
                    regionStartHeight = activeEditableRegion.rectangle().getHeight();
                    event.consume();
                }
                return;
            }
            if (mode != ConfiguratorApplication.ViewerMode.DRAW_REGION) {
                return;
            }
            dragStart = new ViewerPoint(event.getX(), event.getY());
            draftRegion = new Rectangle(event.getX(), event.getY(), 0, 0);
            draftRegion.setFill(Color.color(0.12, 0.48, 0.93, 0.18));
            draftRegion.setStroke(Color.web("#1f7aec"));
            draftRegion.setStrokeWidth(1.5);
            addOverlay(draftRegion);
            event.consume();
        });
        setOnMouseDragged(event -> {
            if (mode == ConfiguratorApplication.ViewerMode.SELECT && regionDragMode != RegionDragMode.NONE && activeEditableRegion != null) {
                updateEditableRegionDrag(event.getX(), event.getY());
                updateDraftRegion.update(activeEditableRegion.type(), screenRegion(activeEditableRegion.rectangle()), false);
                event.consume();
                return;
            }
            if (mode != ConfiguratorApplication.ViewerMode.DRAW_REGION || dragStart == null || draftRegion == null) {
                return;
            }
            var x = Math.min(dragStart.x(), event.getX());
            var y = Math.min(dragStart.y(), event.getY());
            draftRegion.setX(x);
            draftRegion.setY(y);
            draftRegion.setWidth(Math.abs(event.getX() - dragStart.x()));
            draftRegion.setHeight(Math.abs(event.getY() - dragStart.y()));
            event.consume();
        });
        setOnMouseReleased(event -> {
            if (mode == ConfiguratorApplication.ViewerMode.SELECT && regionDragMode != RegionDragMode.NONE) {
                if (activeEditableRegion != null) {
                    updateDraftRegion.update(activeEditableRegion.type(), screenRegion(activeEditableRegion.rectangle()), true);
                }
                regionDragMode = RegionDragMode.NONE;
                activeEditableRegion = null;
                updateSelectCursor(event.getX(), event.getY());
                event.consume();
                return;
            }
            if (mode != ConfiguratorApplication.ViewerMode.DRAW_REGION || dragStart == null) {
                return;
            }
            var end = new ViewerPoint(event.getX(), event.getY());
            var startImage = mapper.screenToImage(dragStart);
            var endImage = mapper.screenToImage(end);
            var x = Math.min(startImage.x(), endImage.x());
            var y = Math.min(startImage.y(), endImage.y());
            var width = Math.abs(endImage.x() - startImage.x());
            var height = Math.abs(endImage.y() - startImage.y());
            dragStart = null;
            draftRegion = null;
            if (width > 0 && height > 0) {
                applyDrawnRegion.accept(roundedRegion(x, y, width, height));
            }
            event.consume();
        });
        setOnMouseMoved(event -> {
            if (mode == ConfiguratorApplication.ViewerMode.SELECT) {
                updateSelectCursor(event.getX(), event.getY());
            }
        });
        setOnMouseExited(event -> {
            if (mode == ConfiguratorApplication.ViewerMode.SELECT && regionDragMode == RegionDragMode.NONE) {
                setCursor(Cursor.DEFAULT);
            }
        });
        setOnMouseClicked(event -> {
            if (mode == ConfiguratorApplication.ViewerMode.DRAW_REGION) {
                event.consume();
                return;
            }
            var point = mapper.screenToImage(new ViewerPoint(event.getX(), event.getY()));
            setUserData(point);
        });
    }

    private Cursor cursorForMode(ConfiguratorApplication.ViewerMode mode) {
        return switch (mode) {
            case PAN -> Cursor.MOVE;
            case DRAW_REGION -> Cursor.CROSSHAIR;
            case SELECT -> Cursor.DEFAULT;
        };
    }

    private void updateSelectCursor(double x, double y) {
        setCursor(cursorForRegionDragMode(hitEditableRegion(x, y).mode()));
    }

    private Cursor cursorForRegionDragMode(RegionDragMode dragMode) {
        return switch (dragMode) {
            case MOVE -> Cursor.MOVE;
            case LEFT, RIGHT -> Cursor.H_RESIZE;
            case TOP, BOTTOM -> Cursor.V_RESIZE;
            case TOP_LEFT, BOTTOM_RIGHT -> Cursor.NW_RESIZE;
            case TOP_RIGHT, BOTTOM_LEFT -> Cursor.NE_RESIZE;
            case NONE -> Cursor.DEFAULT;
        };
    }

    private RegionHit hitEditableRegion(double x, double y) {
        for (int i = editableRegions.size() - 1; i >= 0; i--) {
            var editableRegion = editableRegions.get(i);
            var dragMode = hitRegion(editableRegion.rectangle(), x, y);
            if (dragMode != RegionDragMode.NONE) {
                activeEditableRegion = editableRegion;
                return new RegionHit(editableRegion, dragMode);
            }
        }
        activeEditableRegion = null;
        return new RegionHit(null, RegionDragMode.NONE);
    }

    private RegionDragMode hitRegion(Rectangle rectangle, double x, double y) {
        if (rectangle == null || rectangle.getWidth() <= 0 || rectangle.getHeight() <= 0) {
            return RegionDragMode.NONE;
        }
        var left = rectangle.getX();
        var top = rectangle.getY();
        var right = left + rectangle.getWidth();
        var bottom = top + rectangle.getHeight();
        var withinExpanded = x >= left - REGION_HIT_TOLERANCE
            && x <= right + REGION_HIT_TOLERANCE
            && y >= top - REGION_HIT_TOLERANCE
            && y <= bottom + REGION_HIT_TOLERANCE;
        if (!withinExpanded) {
            return RegionDragMode.NONE;
        }
        var nearLeft = Math.abs(x - left) <= REGION_HIT_TOLERANCE;
        var nearRight = Math.abs(x - right) <= REGION_HIT_TOLERANCE;
        var nearTop = Math.abs(y - top) <= REGION_HIT_TOLERANCE;
        var nearBottom = Math.abs(y - bottom) <= REGION_HIT_TOLERANCE;
        if (nearLeft && nearTop) {
            return RegionDragMode.TOP_LEFT;
        }
        if (nearRight && nearTop) {
            return RegionDragMode.TOP_RIGHT;
        }
        if (nearLeft && nearBottom) {
            return RegionDragMode.BOTTOM_LEFT;
        }
        if (nearRight && nearBottom) {
            return RegionDragMode.BOTTOM_RIGHT;
        }
        if (nearLeft) {
            return RegionDragMode.LEFT;
        }
        if (nearRight) {
            return RegionDragMode.RIGHT;
        }
        if (nearTop) {
            return RegionDragMode.TOP;
        }
        if (nearBottom) {
            return RegionDragMode.BOTTOM;
        }
        if (x >= left && x <= right && y >= top && y <= bottom) {
            return RegionDragMode.MOVE;
        }
        return RegionDragMode.NONE;
    }

    private void updateEditableRegionDrag(double x, double y) {
        var dx = x - regionDragStartX;
        var dy = y - regionDragStartY;
        var left = regionStartX;
        var top = regionStartY;
        var right = regionStartX + regionStartWidth;
        var bottom = regionStartY + regionStartHeight;
        switch (regionDragMode) {
            case MOVE -> {
                left = regionStartX + dx;
                right = left + regionStartWidth;
                top = regionStartY + dy;
                bottom = top + regionStartHeight;
            }
            case LEFT, TOP_LEFT, BOTTOM_LEFT -> left = Math.min(right - MIN_REGION_SIZE, regionStartX + dx);
            case RIGHT, TOP_RIGHT, BOTTOM_RIGHT -> right = Math.max(left + MIN_REGION_SIZE, regionStartX + regionStartWidth + dx);
            case TOP -> top = Math.min(bottom - MIN_REGION_SIZE, regionStartY + dy);
            case BOTTOM -> bottom = Math.max(top + MIN_REGION_SIZE, regionStartY + regionStartHeight + dy);
            case NONE -> {
            }
        }
        if (regionDragMode == RegionDragMode.TOP_LEFT || regionDragMode == RegionDragMode.TOP_RIGHT) {
            top = Math.min(bottom - MIN_REGION_SIZE, regionStartY + dy);
        }
        if (regionDragMode == RegionDragMode.BOTTOM_LEFT || regionDragMode == RegionDragMode.BOTTOM_RIGHT) {
            bottom = Math.max(top + MIN_REGION_SIZE, regionStartY + regionStartHeight + dy);
        }
        activeEditableRegion.rectangle().setX(left);
        activeEditableRegion.rectangle().setY(top);
        activeEditableRegion.rectangle().setWidth(right - left);
        activeEditableRegion.rectangle().setHeight(bottom - top);
    }

    private RegionDto screenRegion(Rectangle rectangle) {
        var topLeft = mapper.screenToImage(new ViewerPoint(rectangle.getX(), rectangle.getY()));
        var bottomRight = mapper.screenToImage(new ViewerPoint(rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight()));
        return roundedRegion(topLeft.x(), topLeft.y(), bottomRight.x() - topLeft.x(), bottomRight.y() - topLeft.y());
    }

    private RegionDto roundedRegion(double x, double y, double width, double height) {
        return new RegionDto(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
    }

    private record EditableRegion(Rectangle rectangle, RegionTargetType type) {
    }

    private record RegionHit(EditableRegion region, RegionDragMode mode) {
    }

    private enum RegionDragMode {
        NONE,
        MOVE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    @FunctionalInterface
    interface RegionUpdateHandler {
        void update(RegionTargetType targetType, RegionDto region, boolean commit);
    }
}
