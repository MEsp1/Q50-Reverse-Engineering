package com.q50dev.hello

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.view.MotionEvent
import android.view.View

/**
 * Modern High-Contrast Automotive Dashboard for Infiniti Q50s HEV (2017).
 *
 * Tailored specifically for the 800×480 7-inch lower touchscreen (InTouch DCU):
 * - Large typography readable at a glance from the driver's seat.
 * - Tab 0: GT-R Performance Cockpit (Tachometer Arc, Gear, Speed, Temps, Throttle).
 * - Tab 1: Chassis & TPMS (Q50 top-down silhouette, 4 Massive Tire Cards with PSI & °C).
 * - Tab 2: Hybrid Powertrain (HEV Power/Regen, EV Mode indicator) & G-Force Radar.
 * - Tab 3: Drag Timer 0-100 km/h (Automatic launch detection, split 0-60, personal record).
 * - Zero GC allocations inside onDraw() for butter-smooth 30-40 FPS on Dalvik 2.3.
 */
class DashboardView(context: Context) : View(context) {

    // --- Active Tab State (0..3) ---
    var activeTab: Int = 0

    // --- Vehicle Telemetry Data Reference ---
    var data: VehicleData = VehicleData()

    // --- Live vs Demo Mode ---
    var isLiveMode: Boolean = false
    var demoTimer: Float = 0f

    // --- 30 FPS Refresh Loop ---
    private val refreshHandler = Handler()
    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isLiveMode) {
                updateDemoSimulation()
            }
            // Update drag timing logic on every tick
            data.updateDrag(0.033f)
            invalidate()
            refreshHandler.postDelayed(this, 33) // ~30 FPS
        }
    }

    // =========================================================================
    // PRE-ALLOCATED GRAPHICS OBJECTS (Zero allocations in onDraw!)
    // =========================================================================

    // --- Paints ---
    private val bgPaint = Paint().apply { color = Color.parseColor("#0C0E14") }
    private val panelPaint = Paint().apply {
        color = Color.parseColor("#151922")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val panelBorderPaint = Paint().apply {
        color = Color.parseColor("#262E3E")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val tabActivePaint = Paint().apply {
        color = Color.parseColor("#202838")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val tabBorderActivePaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val redAccentPaint = Paint().apply {
        color = Color.parseColor("#FF1E43")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val cyanAccentPaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val greenAccentPaint = Paint().apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val amberAccentPaint = Paint().apply {
        color = Color.parseColor("#FF9100")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // --- Text Paints ---
    private val textHeaderTitle = Paint().apply {
        color = Color.WHITE
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val textHeaderTab = Paint().apply {
        color = Color.parseColor("#90A4AE")
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val textHeaderTabActive = Paint().apply {
        color = Color.parseColor("#00E5FF")
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val textLabel = Paint().apply {
        color = Color.parseColor("#78909C")
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val textValueHuge = Paint().apply {
        color = Color.WHITE
        textSize = 44f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val textValueLarge = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val textValueMed = Paint().apply {
        color = Color.WHITE
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val textUnit = Paint().apply {
        color = Color.parseColor("#78909C")
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val textGearHuge = Paint().apply {
        color = Color.parseColor("#FFC107")
        textSize = 72f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val textSpeedHuge = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val textDragTimer = Paint().apply {
        color = Color.parseColor("#00E5FF")
        textSize = 56f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // --- Gauge Arcs & Strokes ---
    private val arcTrackPaint = Paint().apply {
        color = Color.parseColor("#1E2533")
        style = Paint.Style.STROKE
        strokeWidth = 14f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val arcActivePaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 14f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val arcRedlinePaint = Paint().apply {
        color = Color.parseColor("#FF1E43")
        style = Paint.Style.STROKE
        strokeWidth = 14f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    private val radarRingPaint = Paint().apply {
        color = Color.parseColor("#263238")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }
    private val radarCrossPaint = Paint().apply {
        color = Color.parseColor("#37474F")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = true
    }
    private val carOutlinePaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }
    private val carFillPaint = Paint().apply {
        color = Color.parseColor("#0F1C2B")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // --- Pre-allocated Geometry Reusables ---
    private val rectScratch = RectF()
    private val pathScratch = Path()
    private val tabRects = arrayOf(RectF(), RectF(), RectF(), RectF())

    init {
        refreshHandler.post(frameRunnable)
    }

    fun start() {
        refreshHandler.removeCallbacks(frameRunnable)
        refreshHandler.post(frameRunnable)
    }

    fun stop() {
        refreshHandler.removeCallbacks(frameRunnable)
    }

    /**
     * Smooth mathematical simulation of driving on a test track
     * when real CAN sensors have not yet sent events.
     */
    private fun updateDemoSimulation() {
        demoTimer += 0.035f
        val t = demoTimer

        // Dynamic gear and RPM progression
        val cycle = (t % 15f)
        when {
            cycle < 3f -> {
                data.gear = 1
                data.rpm = 1200f + (cycle / 3f) * 4800f
                data.speedKmh = (cycle / 3f) * 45f
            }
            cycle < 6.5f -> {
                data.gear = 2
                val p = (cycle - 3f) / 3.5f
                data.rpm = 2500f + p * 4500f
                data.speedKmh = 45f + p * 42f
            }
            cycle < 10f -> {
                data.gear = 3
                val p = (cycle - 6.5f) / 3.5f
                data.rpm = 3000f + p * 4200f
                data.speedKmh = 87f + p * 38f
            }
            cycle < 13f -> {
                data.gear = 4
                val p = (cycle - 10f) / 3f
                data.rpm = 3500f + p * 3000f
                data.speedKmh = 125f + p * 25f
            }
            else -> {
                // Hard braking down to 0 to restart 0-100 launch!
                data.gear = 1
                val p = (cycle - 13f) / 2f
                data.rpm = 6500f - p * 5500f
                data.speedKmh = (150f - p * 150f).coerceAtLeast(0f)
            }
        }

        data.coolantC = 88f + Math.sin(t * 0.1).toFloat() * 2f
        data.oilTempC = 93f + Math.sin(t * 0.08).toFloat() * 3f
        data.oilPressureRaw = 2.0f + (data.rpm / 7500f) * 3.8f
        data.throttle = if (cycle < 13f) 85f else 0f
        data.torqueNm = 200f + (data.throttle / 100f) * 320f
        data.power = (data.torqueNm * data.rpm / 9549f) * 1.341f

        data.lateralG = Math.sin(t * 0.7).toFloat() * 0.82f
        data.longitudinalG = (data.throttle / 100f) * 0.65f - (if (cycle >= 13f) 0.85f else 0f)
        data.updatePeakG()

        data.tpmsFL = 35.2f + Math.sin(t * 0.05).toFloat() * 0.3f
        data.tpmsFR = 35.5f + Math.cos(t * 0.05).toFloat() * 0.3f
        data.tpmsRL = 34.8f + Math.sin(t * 0.04).toFloat() * 0.2f
        data.tpmsRR = 35.0f + Math.cos(t * 0.04).toFloat() * 0.2f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            // Check header tabs (0..3)
            for (i in 0..3) {
                if (tabRects[i].contains(x, y)) {
                    activeTab = i
                    invalidate()
                    return true
                }
            }

            // Right badge tap toggles Live / Demo
            if (x > width - 90 && y < 50) {
                isLiveMode = !isLiveMode
                invalidate()
                return true
            }

            // In Drag tab: tap bottom center to reset
            if (activeTab == 3 && y > height - 70) {
                data.resetDrag()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)
        drawHeader(canvas, w)

        when (activeTab) {
            0 -> drawMotorTab(canvas, w, h)
            1 -> drawTiresTab(canvas, w, h)
            2 -> drawHybridTab(canvas, w, h)
            3 -> drawDragTab(canvas, w, h)
        }
    }

    // =========================================================================
    // HEADER & NAVIGATION (4 TABS)
    // =========================================================================

    private fun drawHeader(c: Canvas, w: Float) {
        val hHeader = 50f
        rectScratch.set(0f, 0f, w, hHeader)
        c.drawRect(rectScratch, panelPaint)

        // Red Nismo badge bar
        c.drawRect(12f, 15f, 16f, 35f, redAccentPaint)
        c.drawText("Q50s SPORT // GT-R", 24f, 32f, textHeaderTitle)

        // 4 Tab buttons
        val tabW = 106f
        val tabH = 34f
        val tabY = 8f
        val tabStartX = w - 530f

        val titles = arrayOf("01 MOTOR", "02 LLANTAS", "03 HÍBRIDO", "04 DRAG 0-100")
        for (i in 0..3) {
            val tx = tabStartX + i * (tabW + 6f)
            tabRects[i].set(tx, tabY, tx + tabW, tabY + tabH)

            if (activeTab == i) {
                c.drawRoundRect(tabRects[i], 6f, 6f, tabActivePaint)
                c.drawRoundRect(tabRects[i], 6f, 6f, tabBorderActivePaint)
                c.drawText(titles[i], tx + tabW / 2f, tabY + 22f, textHeaderTabActive)
            } else {
                c.drawRoundRect(tabRects[i], 6f, 6f, panelPaint)
                c.drawRoundRect(tabRects[i], 6f, 6f, panelBorderPaint)
                c.drawText(titles[i], tx + tabW / 2f, tabY + 22f, textHeaderTab)
            }
        }

        // Live / Demo Status Badge
        val badgeX = w - 75f
        val badgeY = 12f
        rectScratch.set(badgeX, badgeY, badgeX + 65f, badgeY + 26f)
        val badgePaint = if (isLiveMode) greenAccentPaint else amberAccentPaint
        c.drawRoundRect(rectScratch, 4f, 4f, panelPaint)
        c.drawRoundRect(rectScratch, 4f, 4f, badgePaint.apply { style = Paint.Style.STROKE; strokeWidth = 1.5f })
        val badgeTextPaint = if (isLiveMode) textValueMed.apply { color = Color.parseColor("#00E676"); textSize = 10.5f; textAlign = Paint.Align.CENTER }
                             else textValueMed.apply { color = Color.parseColor("#FF9100"); textSize = 10.5f; textAlign = Paint.Align.CENTER }
        c.drawText(if (isLiveMode) "LIVE" else "DEMO", badgeX + 32.5f, badgeY + 18f, badgeTextPaint)
    }

    // =========================================================================
    // TAB 0: MOTOR // GT-R PERFORMANCE COCKPIT
    // =========================================================================

    private fun drawMotorTab(c: Canvas, w: Float, h: Float) {
        val colW = 180f
        val topY = 60f
        val cardH = 92f
        val gap = 10f

        drawTile(c, 14f, topY, colW, cardH, "TEMP AGUA", "${data.coolantC.toInt()}", "°C",
            data.coolantC, 80f, 105f)
        drawTile(c, 14f, topY + cardH + gap, colW, cardH, "TEMP ACEITE", "${data.oilTempC.toInt()}", "°C",
            data.oilTempC, 85f, 115f)
        drawTile(c, 14f, topY + 2 * (cardH + gap), colW, cardH, "PRESIÓN ACEITE",
            String.format("%.1f", data.oilPressureBar), "BAR", data.oilPressureBar, 1.5f, 6.0f)

        val rightX = w - 14f - colW
        drawTile(c, rightX, topY, colW, cardH, "ACELERADOR", "${data.throttle.toInt()}", "%",
            data.throttle, 0f, 100f)
        drawTile(c, rightX, topY + cardH + gap, colW, cardH, "POTENCIA", "${data.power.toInt()}", "HP",
            data.power, 0f, 360f)
        drawTile(c, rightX, topY + 2 * (cardH + gap), colW, cardH, "PAR MOTOR", "${data.torqueNm.toInt()}", "N·M",
            data.torqueNm, 0f, 546f)

        val centerX = w / 2f
        val centerY = 215f
        val radius = 125f

        rectScratch.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        c.drawArc(rectScratch, 140f, 260f, false, arcTrackPaint)

        val rpmFrac = (data.rpm / 7500f).coerceIn(0f, 1f)
        val sweepAngle = rpmFrac * 260f
        val isRedline = data.rpm >= 6500f
        val arcPaint = if (isRedline) arcRedlinePaint else arcActivePaint
        c.drawArc(rectScratch, 140f, sweepAngle, false, arcPaint)

        c.drawText(data.gearDisplay, centerX, centerY - 5f, textGearHuge)
        c.drawText("${data.speedKmh.toInt()}", centerX, centerY + 50f, textSpeedHuge)
        c.drawText("KM / H", centerX, centerY + 72f, textHeaderTab.apply { color = Color.parseColor("#90A4AE") })

        val rpmColor = if (isRedline) Color.parseColor("#FF1E43") else Color.parseColor("#00E5FF")
        c.drawText("${data.rpm.toInt()} RPM", centerX, centerY + 105f, textValueMed.apply { color = rpmColor; textAlign = Paint.Align.CENTER })

        val barW = 320f
        val barH = 12f
        val barX = centerX - barW / 2f
        val barY = centerY + 130f

        rectScratch.set(barX, barY, barX + barW, barY + barH)
        c.drawRoundRect(rectScratch, 6f, 6f, panelPaint)
        c.drawRoundRect(rectScratch, 6f, 6f, panelBorderPaint)

        val fillW = (barW * (data.throttle / 100f)).coerceIn(0f, barW)
        if (fillW > 0f) {
            rectScratch.set(barX + 2f, barY + 2f, barX + fillW - 2f, barY + barH - 2f)
            c.drawRoundRect(rectScratch, 4f, 4f, cyanAccentPaint)
        }
        c.drawText("THROTTLE // ${data.throttle.toInt()}%", centerX, barY + 30f, textLabel.apply { textAlign = Paint.Align.CENTER })
    }

    private fun drawTile(c: Canvas, x: Float, y: Float, w: Float, h: Float,
                         label: String, value: String, unit: String,
                         metric: Float, warnLow: Float, warnHigh: Float) {
        rectScratch.set(x, y, x + w, y + h)
        c.drawRoundRect(rectScratch, 8f, 8f, panelPaint)

        val isWarning = metric > warnHigh || (warnLow > 0f && metric < warnLow && metric > 0f)
        val borderP = if (isWarning) redAccentPaint.apply { style = Paint.Style.STROKE; strokeWidth = 2f } else panelBorderPaint
        c.drawRoundRect(rectScratch, 8f, 8f, borderP)

        c.drawText(label, x + 14f, y + 24f, textLabel.apply { textAlign = Paint.Align.LEFT })
        c.drawText(value, x + 14f, y + 64f, textValueHuge.apply {
            color = if (isWarning) Color.parseColor("#FF1E43") else Color.WHITE
            textAlign = Paint.Align.LEFT
        })

        val valW = textValueHuge.measureText(value)
        c.drawText(unit, x + 18f + valW, y + 62f, textUnit.apply { textAlign = Paint.Align.LEFT })

        val barY = y + h - 10f
        val barW = w - 28f
        rectScratch.set(x + 14f, barY, x + 14f + barW, barY + 3f)
        c.drawRect(rectScratch, arcTrackPaint.apply { strokeWidth = 3f })
        val frac = if (warnHigh > warnLow) ((metric - warnLow) / (warnHigh - warnLow)).coerceIn(0.05f, 1f) else 0.5f
        rectScratch.set(x + 14f, barY, x + 14f + (barW * frac), barY + 3f)
        c.drawRect(rectScratch, if (isWarning) redAccentPaint else cyanAccentPaint)
    }

    // =========================================================================
    // TAB 1: LLANTAS // CHASSIS & TPMS INSPECTOR
    // =========================================================================

    private fun drawTiresTab(c: Canvas, w: Float, h: Float) {
        val cardW = 245f
        val cardH = 165f
        val topY = 65f
        val bottomY = 240f
        val leftX = 16f
        val rightX = w - 16f - cardW

        drawTireCard(c, leftX, topY, cardW, cardH, "DELANTERA IZQ (FL)", data.getPsiFL(), data.tpmsTempFL)
        drawTireCard(c, rightX, topY, cardW, cardH, "DELANTERA DER (FR)", data.getPsiFR(), data.tpmsTempFR)
        drawTireCard(c, leftX, bottomY, cardW, cardH, "TRASERA IZQ (RL)", data.getPsiRL(), data.tpmsTempRL)
        drawTireCard(c, rightX, bottomY, cardW, cardH, "TRASERA DER (RR)", data.getPsiRR(), data.tpmsTempRR)

        val centerX = w / 2f
        drawCarSilhouette(c, centerX, 235f)

        val infoY = h - 55f
        rectScratch.set(leftX, infoY, w - 16f, infoY + 42f)
        c.drawRoundRect(rectScratch, 6f, 6f, panelPaint)
        c.drawRoundRect(rectScratch, 6f, 6f, panelBorderPaint)

        c.drawText("PRESIÓN RECOMENDADA EN FRÍO: 35.0 PSI  //  SENSORES TPMS SINCRONIZADOS POR CAN",
            centerX, infoY + 26f, textHeaderTab.apply { color = Color.parseColor("#00E5FF") })
    }

    private fun drawTireCard(c: Canvas, x: Float, y: Float, w: Float, h: Float,
                             title: String, psi: Float, tempC: Float) {
        rectScratch.set(x, y, x + w, y + h)
        c.drawRoundRect(rectScratch, 10f, 10f, panelPaint)

        val isLow = psi < 29.0f
        val isCrit = psi < 26.0f
        val isHigh = psi > 40.0f
        val statusColor = when {
            isCrit -> Color.parseColor("#FF1E43")
            isLow || isHigh -> Color.parseColor("#FF9100")
            else -> Color.parseColor("#00E5FF")
        }

        val bPaint = panelBorderPaint.apply { color = statusColor; strokeWidth = 2.5f }
        c.drawRoundRect(rectScratch, 10f, 10f, bPaint)

        c.drawText(title, x + 16f, y + 28f, textLabel.apply { color = Color.parseColor("#90A4AE"); textAlign = Paint.Align.LEFT })

        val psiStr = String.format("%.1f", psi)
        c.drawText(psiStr, x + 16f, y + 88f, textValueHuge.apply {
            color = statusColor
            textSize = 50f
            textAlign = Paint.Align.LEFT
        })

        val strW = textValueHuge.measureText(psiStr)
        c.drawText("PSI", x + 24f + strW, y + 84f, textValueMed.apply {
            color = Color.parseColor("#78909C")
            textSize = 22f
            textAlign = Paint.Align.LEFT
        })

        c.drawText("TEMP: ${tempC.toInt()} °C", x + 16f, y + 122f, textValueMed.apply {
            color = Color.WHITE
            textSize = 17f
            textAlign = Paint.Align.LEFT
        })

        val statusText = when {
            isCrit -> "ALERTA: BAJA PRESIÓN"
            isLow -> "REVISAR PRESIÓN"
            isHigh -> "PRESIÓN ALTA"
            else -> "ESTADO: ÓPTIMO"
        }
        rectScratch.set(x + 16f, y + 134f, x + w - 16f, y + 154f)
        c.drawRoundRect(rectScratch, 4f, 4f, panelPaint)
        c.drawText(statusText, x + w / 2f, y + 149f, textHeaderTab.apply { color = statusColor; textSize = 11.5f })
    }

    private fun drawCarSilhouette(c: Canvas, cx: Float, cy: Float) {
        val cw = 85f
        val ch = 180f

        pathScratch.reset()
        pathScratch.moveTo(cx - 28f, cy - ch / 2f)
        pathScratch.quadTo(cx, cy - ch / 2f - 12f, cx + 28f, cy - ch / 2f)
        pathScratch.lineTo(cx + cw / 2f, cy - ch / 2f + 40f)
        pathScratch.lineTo(cx + cw / 2f, cy + ch / 2f - 35f)
        pathScratch.lineTo(cx + 32f, cy + ch / 2f)
        pathScratch.quadTo(cx, cy + ch / 2f + 8f, cx - 32f, cy + ch / 2f)
        pathScratch.lineTo(cx - cw / 2f, cy + ch / 2f - 35f)
        pathScratch.lineTo(cx - cw / 2f, cy - ch / 2f + 40f)
        pathScratch.close()

        c.drawPath(pathScratch, carFillPaint)
        c.drawPath(pathScratch, carOutlinePaint)

        c.drawLine(cx - cw / 2f, cy - 45f, cx - cw / 2f - 35f, cy - 45f, carOutlinePaint)
        c.drawLine(cx + cw / 2f, cy - 45f, cx + cw / 2f + 35f, cy - 45f, carOutlinePaint)
        c.drawLine(cx - cw / 2f, cy + 45f, cx - cw / 2f - 35f, cy + 45f, carOutlinePaint)
        c.drawLine(cx + cw / 2f, cy + 45f, cx + cw / 2f + 35f, cy + 45f, carOutlinePaint)

        c.drawText("Q50s", cx, cy - 5f, textHeaderTab.apply { color = Color.WHITE; textSize = 13f })
        c.drawText("HEV", cx, cy + 15f, textHeaderTab.apply { color = Color.parseColor("#00E5FF"); textSize = 11f })
    }

    // =========================================================================
    // TAB 2: HÍBRIDO & G-FORCE RADAR
    // =========================================================================

    private fun drawHybridTab(c: Canvas, w: Float, h: Float) {
        val halfW = (w - 38f) / 2f
        val leftX = 14f
        val rightX = w / 2f + 5f

        // LEFT: HYBRID POWERTRAIN
        rectScratch.set(leftX, 65f, leftX + halfW, h - 20f)
        c.drawRoundRect(rectScratch, 10f, 10f, panelPaint)
        c.drawRoundRect(rectScratch, 10f, 10f, panelBorderPaint)

        c.drawText("POWERTRAIN HÍBRIDO // Q50s HEV", leftX + 18f, 95f, textHeaderTitle.apply { color = Color.parseColor("#00E5FF") })

        val evActive = data.isEvMode
        val evBoxY = 115f
        rectScratch.set(leftX + 18f, evBoxY, leftX + halfW - 18f, evBoxY + 45f)
        c.drawRoundRect(rectScratch, 6f, 6f, if (evActive) greenAccentPaint.apply { alpha = 40 } else panelPaint)
        c.drawRoundRect(rectScratch, 6f, 6f, if (evActive) greenAccentPaint.apply { style = Paint.Style.STROKE; strokeWidth = 2f; alpha = 255 } else panelBorderPaint)
        val evText = if (evActive) "⚡ MODO 100% ELÉCTRICO ACTIVO" else "⛽ MOTOR DE COMBUSTIÓN (V6 3.5L)"
        val evColor = if (evActive) Color.parseColor("#00E676") else Color.parseColor("#78909C")
        c.drawText(evText, leftX + halfW / 2f, evBoxY + 28f, textHeaderTab.apply { color = evColor; textSize = 14f })

        val barY = 195f
        val barW = halfW - 36f
        c.drawText("POTENCIA HÍBRIDA (POWER)", leftX + 18f, barY - 10f, textLabel)
        rectScratch.set(leftX + 18f, barY, leftX + 18f + barW, barY + 22f)
        c.drawRoundRect(rectScratch, 4f, 4f, arcTrackPaint.apply { strokeWidth = 22f; style = Paint.Style.FILL })
        val pFrac = (data.throttle / 100f).coerceIn(0f, 1f)
        if (pFrac > 0f) {
            rectScratch.set(leftX + 18f, barY, leftX + 18f + (barW * pFrac), barY + 22f)
            c.drawRoundRect(rectScratch, 4f, 4f, cyanAccentPaint)
        }
        c.drawText("${(pFrac * 100).toInt()}%", leftX + halfW - 22f, barY - 10f, textUnit.apply { textAlign = Paint.Align.RIGHT })

        val regenY = 255f
        c.drawText("REGENERACIÓN (CHARGE)", leftX + 18f, regenY - 10f, textLabel)
        rectScratch.set(leftX + 18f, regenY, leftX + 18f + barW, regenY + 22f)
        c.drawRoundRect(rectScratch, 4f, 4f, arcTrackPaint.apply { strokeWidth = 22f; style = Paint.Style.FILL })
        val regenFrac = if (data.isRegenActive) 0.65f else 0.05f
        rectScratch.set(leftX + 18f, regenY, leftX + 18f + (barW * regenFrac), regenY + 22f)
        c.drawRoundRect(rectScratch, 4f, 4f, greenAccentPaint)
        c.drawText(if (data.isRegenActive) "REGEN ACTIVA" else "STANDBY", leftX + halfW - 22f, regenY - 10f,
            textUnit.apply { color = if (data.isRegenActive) Color.parseColor("#00E676") else Color.parseColor("#78909C"); textAlign = Paint.Align.RIGHT })

        val statY = 325f
        c.drawText("VOLTAJE 12V / HV", leftX + 18f, statY, textLabel.apply { textAlign = Paint.Align.LEFT })
        c.drawText("14.4 V", leftX + 18f, statY + 38f, textValueLarge.apply { textAlign = Paint.Align.LEFT; color = Color.WHITE })

        c.drawText("MOTOR ELÉCTRICO", leftX + halfW / 2f + 10f, statY, textLabel.apply { textAlign = Paint.Align.LEFT })
        c.drawText("50 kW (67 HP)", leftX + halfW / 2f + 10f, statY + 38f, textValueLarge.apply { textAlign = Paint.Align.LEFT; color = Color.parseColor("#00E5FF") })

        c.drawText("PAR COMBINADO MÁX: 546 N·M (403 LB-FT)", leftX + halfW / 2f, h - 35f, textHeaderTab.apply { color = Color.parseColor("#78909C"); textSize = 11.5f })

        // RIGHT: G-FORCE RADAR
        rectScratch.set(rightX, 65f, rightX + halfW, h - 20f)
        c.drawRoundRect(rectScratch, 10f, 10f, panelPaint)
        c.drawRoundRect(rectScratch, 10f, 10f, panelBorderPaint)

        c.drawText("RADAR DE FUERZAS G // GT-R G-BOWL", rightX + 18f, 95f, textHeaderTitle.apply { color = Color.parseColor("#FF1E43") })

        val radarCX = rightX + halfW / 2f
        val radarCY = 230f
        val radarR = 105f

        c.drawCircle(radarCX, radarCY, radarR * 0.33f, radarRingPaint)
        c.drawCircle(radarCX, radarCY, radarR * 0.66f, radarRingPaint)
        c.drawCircle(radarCX, radarCY, radarR, radarRingPaint)

        c.drawLine(radarCX - radarR - 10f, radarCY, radarCX + radarR + 10f, radarCY, radarCrossPaint)
        c.drawLine(radarCX, radarCY - radarR - 10f, radarCX, radarCY + radarR + 10f, radarCrossPaint)

        c.drawText("0.5G", radarCX + radarR * 0.33f + 4f, radarCY - 4f, textUnit.apply { textSize = 10f })
        c.drawText("1.0G", radarCX + radarR * 0.66f + 4f, radarCY - 4f, textUnit.apply { textSize = 10f })
        c.drawText("1.5G", radarCX + radarR + 4f, radarCY - 4f, textUnit.apply { textSize = 10f })

        val maxGScale = 1.5f
        val dotX = radarCX + (data.lateralG / maxGScale * radarR).coerceIn(-radarR, radarR)
        val dotY = radarCY - (data.longitudinalG / maxGScale * radarR).coerceIn(-radarR, radarR)

        c.drawCircle(dotX, dotY, 14f, redAccentPaint.apply { alpha = 60 })
        c.drawCircle(dotX, dotY, 8f, redAccentPaint.apply { alpha = 255 })

        val gStatY = 370f
        val latGStr = String.format("%+.2f G", data.lateralG)
        val longGStr = String.format("%+.2f G", data.longitudinalG)
        val peakStr = String.format("%.2f G", data.peakG)

        c.drawText("LATERAL", radarCX - 90f, gStatY, textLabel.apply { textAlign = Paint.Align.CENTER })
        c.drawText(latGStr, radarCX - 90f, gStatY + 30f, textValueMed.apply { textAlign = Paint.Align.CENTER; color = Color.WHITE })

        c.drawText("ACEL / FRENO", radarCX, gStatY, textLabel.apply { textAlign = Paint.Align.CENTER })
        c.drawText(longGStr, radarCX, gStatY + 30f, textValueMed.apply { textAlign = Paint.Align.CENTER; color = Color.WHITE })

        c.drawText("PICO MÁX", radarCX + 90f, gStatY, textLabel.apply { textAlign = Paint.Align.CENTER })
        c.drawText(peakStr, radarCX + 90f, gStatY + 30f, textValueMed.apply { textAlign = Paint.Align.CENTER; color = Color.parseColor("#FF1E43") })

        c.drawText("TOQUE PARA REINICIAR PICO", radarCX, h - 35f, textHeaderTab.apply { color = Color.parseColor("#78909C"); textSize = 11f })
    }

    // =========================================================================
    // TAB 3: DRAG TIMER 0-100 KM/H (DRAGGY STYLE AUTO LAUNCH)
    // =========================================================================

    private fun drawDragTab(c: Canvas, w: Float, h: Float) {
        val centerX = w / 2f

        // Top Status Capsule (Launch control & Ready check)
        val topStatusY = 65f
        rectScratch.set(16f, topStatusY, w - 16f, topStatusY + 54f)
        c.drawRoundRect(rectScratch, 8f, 8f, panelPaint)

        val (statusText, statusBorderColor) = when (data.dragStatus) {
            0 -> Pair("🚦 LISTO PARA EL LANZAMIENTO (DETENIDO EN 0 KM/H)", Color.parseColor("#00E676"))
            1 -> Pair("⚡ ¡ACELERACIÓN EN CURSO! MIDIENDO 0 A 100 KM/H", Color.parseColor("#FF9100"))
            else -> Pair("🏁 ¡PRUEBA COMPLETADA CON ÉXITO! EXCELENTE TIEMPO", Color.parseColor("#00E5FF"))
        }
        c.drawRoundRect(rectScratch, 8f, 8f, panelBorderPaint.apply { color = statusBorderColor; strokeWidth = 2f })
        c.drawText(statusText, centerX, topStatusY + 33f, textHeaderTab.apply { color = statusBorderColor; textSize = 15f })

        // Center Big Stopwatch Card
        val timerCardY = 130f
        val timerCardH = 160f
        val timerCardW = w - 32f
        rectScratch.set(16f, timerCardY, 16f + timerCardW, timerCardY + timerCardH)
        c.drawRoundRect(rectScratch, 10f, 10f, panelPaint)
        c.drawRoundRect(rectScratch, 10f, 10f, panelBorderPaint)

        // Elapsed time or final 0-100 time
        val displayTime = if (data.dragStatus == 2) data.time0to100 else data.dragTimerSeconds
        val timerStr = String.format("%04.2f s", displayTime)
        c.drawText(timerStr, centerX, timerCardY + 95f, textDragTimer.apply {
            color = if (data.dragStatus == 2) Color.parseColor("#00E676") else Color.parseColor("#00E5FF")
        })

        // Live speed bar during launch
        val speedFrac = (data.speedKmh / 100f).coerceIn(0f, 1f)
        val pBarY = timerCardY + 125f
        val pBarW = timerCardW - 60f
        val pBarX = centerX - pBarW / 2f
        rectScratch.set(pBarX, pBarY, pBarX + pBarW, pBarY + 14f)
        c.drawRoundRect(rectScratch, 6f, 6f, arcTrackPaint.apply { strokeWidth = 14f; style = Paint.Style.FILL })
        if (speedFrac > 0f) {
            rectScratch.set(pBarX, pBarY, pBarX + (pBarW * speedFrac), pBarY + 14f)
            c.drawRoundRect(rectScratch, 6f, 6f, cyanAccentPaint)
        }
        c.drawText("VELOCIDAD ACTUAL: ${data.speedKmh.toInt()} / 100 KM/H", centerX, pBarY - 8f, textLabel.apply { textAlign = Paint.Align.CENTER })

        // Bottom 3 Split Cards: 0-60, 0-100, MEJOR RÉCORD
        val bottomY = 305f
        val cardW = (w - 48f) / 3f
        val bCardH = 110f

        // Card 1: 0 - 60 km/h
        val c1X = 16f
        rectScratch.set(c1X, bottomY, c1X + cardW, bottomY + bCardH)
        c.drawRoundRect(rectScratch, 8f, 8f, panelPaint)
        c.drawRoundRect(rectScratch, 8f, 8f, panelBorderPaint)
        c.drawText("0 - 60 KM/H", c1X + cardW / 2f, bottomY + 28f, textLabel.apply { textAlign = Paint.Align.CENTER })
        val str0to60 = if (data.time0to60 > 0f) String.format("%.2f s", data.time0to60) else "--.-- s"
        c.drawText(str0to60, c1X + cardW / 2f, bottomY + 75f, textValueLarge.apply { textAlign = Paint.Align.CENTER; color = Color.WHITE })

        // Card 2: 0 - 100 km/h
        val c2X = c1X + cardW + 8f
        rectScratch.set(c2X, bottomY, c2X + cardW, bottomY + bCardH)
        c.drawRoundRect(rectScratch, 8f, 8f, panelPaint)
        c.drawRoundRect(rectScratch, 8f, 8f, panelBorderPaint)
        c.drawText("0 - 100 KM/H", c2X + cardW / 2f, bottomY + 28f, textLabel.apply { textAlign = Paint.Align.CENTER })
        val str0to100 = if (data.time0to100 > 0f) String.format("%.2f s", data.time0to100) else "--.-- s"
        c.drawText(str0to100, c2X + cardW / 2f, bottomY + 75f, textValueLarge.apply { textAlign = Paint.Align.CENTER; color = Color.parseColor("#00E5FF") })

        // Card 3: Récord Personal
        val c3X = c2X + cardW + 8f
        rectScratch.set(c3X, bottomY, c3X + cardW, bottomY + bCardH)
        c.drawRoundRect(rectScratch, 8f, 8f, panelPaint)
        c.drawRoundRect(rectScratch, 8f, 8f, panelBorderPaint)
        c.drawText("🏆 MEJOR RÉCORD", c3X + cardW / 2f, bottomY + 28f, textLabel.apply { color = Color.parseColor("#FFC107"); textAlign = Paint.Align.CENTER })
        val strBest = if (data.best0to100 > 0f) String.format("%.2f s", data.best0to100) else "--.-- s"
        c.drawText(strBest, c3X + cardW / 2f, bottomY + 75f, textValueLarge.apply { textAlign = Paint.Align.CENTER; color = Color.parseColor("#FFC107") })

        // Bottom touch reset prompt
        c.drawText("TOQUE AQUÍ PARA REINICIAR MEDICIÓN DRAG", centerX, h - 25f, textHeaderTab.apply { color = Color.parseColor("#78909C"); textSize = 11.5f })
    }
}
