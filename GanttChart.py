class GanttChartPanel(CardFrame):
    """
    Renders the Gantt chart with animated growing blocks and a time axis.
    """

    BAR_H = 68

    def __init__(self, parent):
        super().__init__(parent)
        self._gantt_canvas = None
        self._time_canvas  = None
        self._build()

    def _build(self):
        SectionHeader(self, "③", "GANTT CHART").pack(
            fill="x", padx=18, pady=(16, 8))

        self._gantt_canvas = tk.Canvas(self,
            bg=AppTheme.SURFACE, height=self.BAR_H,
            highlightthickness=0)
        self._gantt_canvas.pack(fill="x", padx=18, pady=(0, 4))

        self._time_canvas = tk.Canvas(self,
            bg=AppTheme.CARD, height=26,
            highlightthickness=0)
        self._time_canvas.pack(fill="x", padx=18, pady=(0, 16))

        self._gantt_canvas.after(60, lambda:
            self._draw_placeholder("Run FCFS to see the Gantt chart"))

    # ── Public API ───────────────────────────────────────────────────────────

    def animate(self, processes: list[Process], callback):
        """Draw all Gantt blocks one by one, then draw the time axis."""
        self._gantt_canvas.delete("all")
        self._time_canvas.delete("all")
        self._gantt_canvas.update_idletasks()

        bar_w      = max(self._gantt_canvas.winfo_width(), 600)
        end_time   = processes[-1].ct
        first_time = processes[0].start if processes[0].start > 0 else 0
        total_span = max(end_time - first_time, 1)

        segments   = self._build_segments(processes, first_time, total_span, bar_w)

        def draw_segment(idx):
            if idx >= len(segments):
                self._draw_time_axis(processes, total_span, first_time, bar_w)
                if callback:
                    callback()
                return
            seg = segments[idx]
            if seg["kind"] == "idle":
                self._gantt_canvas.create_rectangle(
                    seg["x"], 0, seg["x"] + seg["w"], self.BAR_H,
                    fill="#2d333b", outline="#0d1117")
                self._gantt_canvas.create_text(
                    seg["x"] + seg["w"] // 2, self.BAR_H // 2,
                    text="IDLE", fill=AppTheme.MUTED, font=("Consolas", 8))
                self.after(80, lambda: draw_segment(idx + 1))
            else:
                p = seg["proc"]
                self._animate_block_grow(
                    seg["x"], seg["w"], AppTheme.PROC_COLORS[p.pid], p,
                    done_cb=lambda: self.after(60, lambda: draw_segment(idx + 1)))

        draw_segment(0)

    def reset(self):
        self._gantt_canvas.delete("all")
        self._time_canvas.delete("all")
        self._draw_placeholder("Run FCFS to see the Gantt chart")

    # ── Private helpers ──────────────────────────────────────────────────────

    def _build_segments(self, processes, first_time, total_span, bar_w):
        segments = []
        prev_end = first_time

        if processes[0].start > 0:
            w = max(int((processes[0].start / total_span) * bar_w), 10)
            segments.append({"kind": "idle", "w": w, "x": 0})
            prev_end = processes[0].start

        x = sum(s["w"] for s in segments)
        for p in processes:
            if p.start > prev_end:
                gap = p.start - prev_end
                gap_w = max(int((gap / total_span) * bar_w), 10)
                segments.append({"kind": "idle", "w": gap_w, "x": x})
                x += gap_w
            blk_w = max(int((p.burst / total_span) * bar_w), 32)
            segments.append({"kind": "proc", "proc": p, "w": blk_w, "x": x})
            x       += blk_w
            prev_end = p.ct

        return segments

    def _animate_block_grow(self, sx, sw, color, proc, done_cb, step=0):
        STEPS      = 12
        current_w  = int(sw * (step + 1) / STEPS)
        tag        = f"grow_{sx}"
        self._gantt_canvas.delete(tag)
        self._gantt_canvas.create_rectangle(
            sx, 0, sx + current_w, self.BAR_H,
            fill=color, outline="#0d1117", width=1, tags=tag)
        if step + 1 >= STEPS:
            self._gantt_canvas.create_text(
                sx + sw // 2, 24, text=proc.pid,
                fill="#fff", font=("Consolas", 9, "bold"))
            self._gantt_canvas.create_text(
                sx + sw // 2, 46, text=f"{proc.burst}ms",
                fill="white", font=("Consolas", 7))
            done_cb()
        else:
            self.after(18, lambda: self._animate_block_grow(
                sx, sw, color, proc, done_cb, step + 1))

    def _draw_time_axis(self, processes, total_span, first_time, bar_w):
        ticks = set()
        for p in processes:
            ticks.add(p.start)
            ticks.add(p.ct)
        if processes[0].start == 0:
            ticks.add(0)
        prev_px = -30
        for t in sorted(ticks):
            px = int((t - first_time) / total_span * bar_w)
            if px - prev_px > 18:
                self._time_canvas.create_line(px, 0, px, 6, fill=AppTheme.BORDER)
                self._time_canvas.create_text(px, 17,
                    text=str(t), fill=AppTheme.MUTED, font=("Consolas", 7))
                prev_px = px

    def _draw_placeholder(self, text: str):
        c = self._gantt_canvas
        c.delete("all")
        c.update_idletasks()
        w = max(c.winfo_width(), 400)
        c.create_rectangle(0, 0, w, self.BAR_H, fill=AppTheme.SURFACE, outline="")
        c.create_text(w // 2, self.BAR_H // 2,
            text=text, fill=AppTheme.MUTED,
            font=("Consolas", 9), justify="center")