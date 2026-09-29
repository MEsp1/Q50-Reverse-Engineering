package com.q50gtr.plus.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.q50gtr.plus.data.DataHub;
import com.q50gtr.plus.data.VehicleData;

import java.util.Calendar;

/**
 * Вся панель: статусная строка сверху, приборы посередине, вкладки снизу —
 * как в штатном InTouch. Одна View, один onDraw, никаких дочерних вью.
 */
public final class DashboardView extends View implements Runnable {

    public static final float DESIGN_W = 840f;
    public static final float DESIGN_H = 480f;

    private static final float TOP_H = 42f;
    private static final float TABS_H = 46f;
    private static final float CONTENT_H = DESIGN_H - TOP_H - TABS_H;

    private static final long FRAME_MS = 100L;
    private static final float SWIPE_COMMIT = 70f;

    private final Theme theme = new Theme();
    private final DataHub hub;
    private final Page[] pages = new Page[]{new TripEcoPage(), new DragStripPage(), new TrackPage(), new DynoLivePage()};
    private final Calendar calendar = Calendar.getInstance();

    private int page;
    private float dragX;
    private boolean dragging;
    private float downX;
    private float downY;
    private boolean running;
    private final float touchSlop;

    public DashboardView(Context context, DataHub hub) {
        super(context);
        this.hub = hub;
        setBackgroundColor(Theme.BG_BOTTOM);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void start() {
        if (!running) {
            running = true;
            post(this);
        }
    }

    public void stop() {
        running = false;
        removeCallbacks(this);
    }

    public void run() {
        if (!running) {
            return;
        }
        hub.poll(System.currentTimeMillis());
        invalidate();
        postDelayed(this, FRAME_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    /* ------------------------------------------------------------------ */

    @Override
    protected void onDraw(Canvas canvas) {
        VehicleData d = hub.getData();
        Theme t = theme;

        float sx = getWidth() / DESIGN_W;
        float sy = getHeight() / DESIGN_H;

        int base = canvas.save();
        canvas.scale(sx, sy);

        t.rect.set(0f, 0f, DESIGN_W, DESIGN_H);
        canvas.drawRect(t.rect, t.background(DESIGN_H));

        drawTopBar(canvas, d);

        int clip = canvas.save();
        canvas.clipRect(0f, TOP_H, DESIGN_W, TOP_H + CONTENT_H);
        canvas.translate(0f, TOP_H);
        if (dragging && dragX != 0f) {
            int neighbour = dragX < 0f ? page + 1 : page - 1;
            drawPage(canvas, d, page, dragX);
            if (neighbour >= 0 && neighbour < pages.length) {
                drawPage(canvas, d, neighbour, dragX + (dragX < 0f ? DESIGN_W : -DESIGN_W));
            }
        } else {
            drawPage(canvas, d, page, 0f);
        }
        canvas.restoreToCount(clip);

        drawTabs(canvas);
        canvas.restoreToCount(base);
    }

    private void drawPage(Canvas canvas, VehicleData d, int index, float offsetX) {
        int save = canvas.save();
        canvas.translate(offsetX, 0f);
        pages[index].draw(canvas, theme, d, DESIGN_W, CONTENT_H);
        canvas.restoreToCount(save);
    }

    /* ------------------------------------------------------------------ */

    private void drawTopBar(Canvas c, VehicleData d) {
        Theme t = theme;
        t.rect.set(0f, 0f, DESIGN_W, TOP_H);
        c.drawRect(t.rect, t.fill(Theme.BAR));
        c.drawLine(0f, TOP_H - 0.5f, DESIGN_W, TOP_H - 0.5f, t.stroke(0xFF1A2534, 1f));

        // Стрелка «назад» — как в штатной оболочке.
        Path p = t.path;
        p.reset();
        p.moveTo(28f, TOP_H / 2f - 7f);
        p.lineTo(21f, TOP_H / 2f);
        p.lineTo(28f, TOP_H / 2f + 7f);
        c.drawPath(p, t.stroke(Theme.LABEL, 2f));

        calendar.setTimeInMillis(System.currentTimeMillis());
        int hh = calendar.get(Calendar.HOUR_OF_DAY);
        int mm = calendar.get(Calendar.MINUTE);
        StringBuilder sb = new StringBuilder();
        if (hh < 10) {
            sb.append('0');
        }
        sb.append(hh).append(':');
        if (mm < 10) {
            sb.append('0');
        }
        sb.append(mm);
        c.drawText(sb.toString(), DESIGN_W / 2f - 60f, TOP_H / 2f + 6f,
                t.text(Theme.WHITE, 17f, Paint.Align.CENTER, false));

        String temp = d.ambientTemp.hasValue() ? d.ambientTemp.text(0) + " °C" : "-- °C";
        c.drawText(temp, DESIGN_W / 2f + 62f, TOP_H / 2f + 6f,
                t.text(Theme.WHITE, 17f, Paint.Align.CENTER, false));

        // Индикаторы справа: уровень сигнала и Bluetooth.
        float bx = DESIGN_W - 74f;
        for (int i = 0; i < 4; i++) {
            float bh = 4f + i * 3f;
            t.rect.set(bx + i * 5f, TOP_H / 2f + 6f - bh, bx + i * 5f + 3f, TOP_H / 2f + 6f);
            c.drawRect(t.rect, t.fill(Theme.LABEL));
        }
        drawBluetooth(c, DESIGN_W - 34f, TOP_H / 2f, 8f);

        if (hub.isDemoActive()) {
            c.drawText("DEMO", DESIGN_W / 2f + 148f, TOP_H / 2f + 5f,
                    t.text(Theme.ACCENT, 11f, Paint.Align.CENTER, true));
        }
    }

    private void drawBluetooth(Canvas c, float cx, float cy, float r) {
        Theme t = theme;
        Path p = t.path;
        p.reset();
        p.moveTo(cx, cy - r);
        p.lineTo(cx + r * 0.6f, cy - r * 0.45f);
        p.lineTo(cx - r * 0.6f, cy + r * 0.45f);
        p.lineTo(cx, cy + r);
        p.lineTo(cx, cy - r);
        p.moveTo(cx, cy + r);
        p.lineTo(cx + r * 0.6f, cy + r * 0.45f);
        p.lineTo(cx - r * 0.6f, cy - r * 0.45f);
        c.drawPath(p, t.stroke(Theme.LABEL, 1.6f));
    }

    /* ------------------------------------------------------------------ */

    private void drawTabs(Canvas c) {
        Theme t = theme;
        float top = DESIGN_H - TABS_H;
        t.rect.set(0f, top, DESIGN_W, DESIGN_H);
        c.drawRect(t.rect, t.fill(Theme.BAR));
        c.drawLine(0f, top + 0.5f, DESIGN_W, top + 0.5f, t.stroke(0xFF1A2534, 1f));

        arrow(c, 26f, top + TABS_H / 2f, true);
        arrow(c, DESIGN_W - 26f, top + TABS_H / 2f, false);

        for (int i = 0; i < pages.length; i++) {
            float x = tabX(i, pages.length);
            boolean active = i == page;
            if (active) {
                // Скошенная подложка активной вкладки, как в InTouch.
                Path p = t.path;
                p.reset();
                float slant = 12f;
                p.moveTo(x + slant, top + 6f);
                p.lineTo(x + TAB_W - slant, top + 6f);
                p.lineTo(x + TAB_W, DESIGN_H);
                p.lineTo(x, DESIGN_H);
                p.close();
                c.drawPath(p, t.fill(Theme.ACCENT_DEEP));
                c.drawLine(x + slant, top + 6.5f, x + TAB_W - slant, top + 6.5f,
                        t.stroke(Theme.ACCENT, 2f));
            }
            c.drawText(pages[i].title(), x + TAB_W / 2f, top + TABS_H / 2f + 6f,
                    t.text(active ? Theme.WHITE : Theme.LABEL, 15f, Paint.Align.CENTER, active));
        }
    }

    private void arrow(Canvas c, float cx, float cy, boolean left) {
        Theme t = theme;
        Path p = t.path;
        p.reset();
        float d = left ? -1f : 1f;
        p.moveTo(cx - d * 4f, cy - 7f);
        p.lineTo(cx + d * 4f, cy);
        p.lineTo(cx - d * 4f, cy + 7f);
        c.drawPath(p, t.stroke(Theme.LABEL, 2f));
    }

    private static final float TAB_W = 196f;
    private static final float TAB_GAP = 8f;

    private static float tabX(int index, int totalTabs) {
        float total = TAB_W * totalTabs + TAB_GAP * (totalTabs - 1);
        return (DESIGN_W - total) / 2f + index * (TAB_W + TAB_GAP);
    }

    /* ------------------------------------------------------------------ */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float sx = getWidth() / DESIGN_W;
        float sy = getHeight() / DESIGN_H;
        float x = event.getX() / (sx <= 0f ? 1f : sx);
        float y = event.getY() / (sy <= 0f ? 1f : sy);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                dragX = 0f;
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX;
                float dy = y - downY;
                float slop = touchSlop / (sx <= 0f ? 1f : sx);
                if (!dragging && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)
                        && downY > TOP_H && downY < DESIGN_H - TABS_H) {
                    dragging = true;
                }
                if (dragging) {
                    dragX = clampDrag(dx);
                    invalidate();
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (dragging) {
                    if (dragX <= -SWIPE_COMMIT && page < pages.length - 1) {
                        page++;
                    } else if (dragX >= SWIPE_COMMIT && page > 0) {
                        page--;
                    }
                    dragging = false;
                    dragX = 0f;
                    invalidate();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP && y >= DESIGN_H - TABS_H) {
                    if (x < 52f) {
                        setPage(page - 1);
                    } else if (x > DESIGN_W - 52f) {
                        setPage(page + 1);
                    } else {
                        int tapped = tabAt(x);
                        if (tapped >= 0) {
                            setPage(tapped);
                        }
                    }
                }
                return true;
            }

            default:
                return super.onTouchEvent(event);
        }
    }

    private float clampDrag(float dx) {
        if (dx < 0f && page >= pages.length - 1) {
            return dx * 0.25f;
        }
        if (dx > 0f && page <= 0) {
            return dx * 0.25f;
        }
        return dx;
    }

    private int tabAt(float x) {
        for (int i = 0; i < pages.length; i++) {
            float tx = tabX(i, pages.length);
            if (x >= tx && x <= tx + TAB_W) {
                return i;
            }
        }
        return -1;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int index) {
        if (index >= 0 && index < pages.length && index != page) {
            page = index;
            invalidate();
        }
    }
}
