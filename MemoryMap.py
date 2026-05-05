class MemoryMapPanel(CardFrame):
    """
    Displays the memory layout as an animated vertical bar.
    Owns its own canvas and legend; delegates allocation math to MemoryManager.
    """

    MEM_H = 340

    def __init__(self, parent):
        super().__init__(parent)
        self._manager = MemoryManager()
        self._canvas  = None
        self._legend  = None
        self._done_cb = None
        self._build()

    def _build(self):
        SectionHeader(self, "②", "MEMORY MAP (MB)").pack(
            fill="x", padx=18, pady=(16, 8))

        self._canvas = tk.Canvas(self,
            bg=AppTheme.SURFACE, width=210, height=self.MEM_H,
            highlightthickness=0)
        self._canvas.pack(padx=14, pady=(0, 10))
        self._canvas.after(50, self.draw_idle)

        tk.Frame(self, bg=AppTheme.BORDER, height=1).pack(
            fill="x", padx=14, pady=(0, 10))

        self._legend = tk.Frame(self, bg=AppTheme.CARD)
        self._legend.pack(fill="x", padx=14, pady=(0, 16))

    # ── Public API ───────────────────────────────────────────────────────────

    def draw_idle(self):
        c = self._canvas
        c.delete("all")
        w = c.winfo_width() or 210
        c.create_rectangle(0, 0, w, self.MEM_H, fill=AppTheme.SURFACE, outline="")
        c.create_text(w // 2, self.MEM_H // 2,
            text="Awaiting\nprocesses...",
            fill=AppTheme.MUTED, font=("Consolas", 9), justify="center")

    def animate(self, processes: list[Process], done_callback):
        """Animate memory segments top-to-bottom; call done_callback when done."""
        self._done_cb = done_callback
        segments = self._manager.allocate(processes)
        c = self._canvas
        c.delete("all")
        h = self.MEM_H
        w = c.winfo_width() or 210

        # Pre-compute pixel positions
        positions = []
        y = 0
        for seg in segments:
            seg_h = max(int((seg["mb"] / MemoryManager.TOTAL_MB) * h), 22)
            positions.append((seg, y, seg_h))
            y += seg_h

        def draw_up_to(n):
            c.delete("all")
            for i, (seg, sy, sh) in enumerate(positions):
                if i >= n:
                    break
                c.create_rectangle(0, sy, w, sy + sh,
                    fill=seg["color"], outline="#0d1117", width=1)
                c.create_text(w // 2, sy + sh // 2,
                    text=f"{seg['label']}\n{seg['mb']} MB",
                    fill="#ffffff", font=("Consolas", 8, "bold"),
                    justify="center")
            # Rebuild legend
            for child in self._legend.winfo_children():
                child.destroy()
            for i, (seg, _, _) in enumerate(positions):
                if i >= n or not seg["is_process"]:
                    continue
                row = tk.Frame(self._legend, bg=AppTheme.CARD)
                row.pack(anchor="w", pady=2)
                cv2 = tk.Canvas(row, width=10, height=10,
                    bg=AppTheme.CARD, highlightthickness=0)
                cv2.pack(side="left", padx=(0, 5))
                cv2.create_rectangle(0, 0, 10, 10,
                    fill=seg["color"], outline="")
                tk.Label(row, text=f"{seg['label']} — {seg['mb']} MB",
                    bg=AppTheme.CARD, fg=AppTheme.MUTED,
                    font=("Consolas", 8)).pack(side="left")

        def animate_step(n=0):
            draw_up_to(n + 1)
            if n + 1 < len(positions):
                self.after(110, lambda: animate_step(n + 1))
            elif self._done_cb:
                self.after(200, self._done_cb)

        animate_step(0)

    def reset(self):
        self.draw_idle()
        for child in self._legend.winfo_children():
            child.destroy()