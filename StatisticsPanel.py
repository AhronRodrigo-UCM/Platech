class StatisticsPanel(CardFrame):
    """
    Displays per-process metrics in a Treeview table and
    shows animated average WT / TAT / CT metric boxes.
    """

    COLUMNS = ("Process", "Arrival (ms)", "Burst (ms)",
               "Completion (ms)", "Waiting (ms)", "Turnaround (ms)")
    COL_WIDTHS = [100, 120, 110, 140, 120, 145]

    def __init__(self, parent):
        super().__init__(parent)
        self._tree       = None
        self._avg_labels = {}
        self._build()

    def _build(self):
        SectionHeader(self, "④", "PROCESS STATISTICS  —  WT · CT · TAT").pack(
            fill="x", padx=18, pady=(16, 8))

        wrap = tk.Frame(self, bg=AppTheme.CARD)
        wrap.pack(fill="x", padx=18, pady=(0, 12))

        self._tree = ttk.Treeview(wrap, columns=self.COLUMNS,
            show="headings", height=5)
        for col, cw in zip(self.COLUMNS, self.COL_WIDTHS):
            self._tree.heading(col, text=col)
            self._tree.column(col, width=cw, anchor="center")
        self._tree.pack(fill="x")
        self._populate_blanks()

        tk.Frame(self, bg=AppTheme.BORDER, height=1).pack(
            fill="x", padx=18, pady=(4, 14))
        self._build_avg_boxes()

    def _build_avg_boxes(self):
        row = tk.Frame(self, bg=AppTheme.CARD)
        row.pack(fill="x", padx=18, pady=(0, 20))
        for key, label, color in [
            ("wt",  "Avg Waiting Time",    AppTheme.WARNING),
            ("tat", "Avg Turnaround Time", AppTheme.SUCCESS),
            ("ct",  "Avg Completion Time", AppTheme.ACCENT),
        ]:
            box = tk.Frame(row, bg=AppTheme.SURFACE,
                highlightbackground=AppTheme.BORDER, highlightthickness=1,
                padx=18, pady=14)
            box.pack(side="left", expand=True, fill="x", padx=(0, 10))
            tk.Label(box, text=label.upper(), bg=AppTheme.SURFACE, fg=AppTheme.MUTED,
                font=("Consolas", 7, "bold")).pack(anchor="w")
            lbl = tk.Label(box, text="— ms", bg=AppTheme.SURFACE, fg=color,
                font=AppTheme.FONT_METRIC)
            lbl.pack(anchor="w", pady=(6, 0))
            self._avg_labels[key] = lbl

    def _populate_blanks(self):
        for row in self._tree.get_children():
            self._tree.delete(row)
        for pid in Process.VALID_IDS:
            self._tree.insert("", "end",
                values=(pid, "—", "—", "—", "—", "—"),
                tags=(pid.lower(),))
            self._tree.tag_configure(pid.lower(),
                foreground=AppTheme.PROC_COLORS[pid])

    # ── Public API ───────────────────────────────────────────────────────────

    def reset(self):
        self._populate_blanks()
        for lbl in self._avg_labels.values():
            lbl.config(text="— ms")

    def animate(self, processes: list[Process], callback=None):
        """Insert rows one at a time, then count up the averages."""
        # Clear and re-add blank rows for unscheduled processes
        for row in self._tree.get_children():
            self._tree.delete(row)
        scheduled_ids = {p.pid for p in processes}
        for pid in Process.VALID_IDS:
            if pid not in scheduled_ids:
                self._tree.insert("", "end",
                    values=(pid, "—", "—", "—", "—", "—"),
                    tags=(pid.lower(),))
                self._tree.tag_configure(pid.lower(),
                    foreground=AppTheme.PROC_COLORS[pid])

        def insert_row(idx):
            if idx >= len(processes):
                self._animate_averages(processes, callback)
                return
            p = processes[idx]
            self._tree.insert("", "end",
                values=(p.pid, p.arrival, p.burst, p.ct, p.wt, p.tat),
                tags=(p.pid.lower(),))
            self._tree.tag_configure(p.pid.lower(),
                foreground=AppTheme.PROC_COLORS[p.pid])
            self.after(130, lambda: insert_row(idx + 1))

        insert_row(0)

    def _animate_averages(self, processes: list[Process], callback):
        n      = len(processes)
        target = {
            "wt":  round(sum(p.wt  for p in processes) / n, 2),
            "tat": round(sum(p.tat for p in processes) / n, 2),
            "ct":  round(sum(p.ct  for p in processes) / n, 2),
        }
        STEPS = 20

        def tick(step):
            frac = (step + 1) / STEPS
            for key, lbl in self._avg_labels.items():
                lbl.config(text=f"{round(target[key] * frac, 2)} ms")
            if step + 1 < STEPS:
                self.after(30, lambda: tick(step + 1))
            else:
                for key, lbl in self._avg_labels.items():
                    lbl.config(text=f"{target[key]} ms")
                if callback:
                    self.after(200, callback)

        tick(0)