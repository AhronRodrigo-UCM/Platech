class ProcessQueuePanel(CardFrame):
    """
    Renders the input table for up to 5 processes.
    Owns validation logic and exposes flash_rows() for animation.
    """

    def __init__(self, parent, on_run, on_reset):
        super().__init__(parent)
        self._on_run    = on_run
        self._on_reset  = on_reset
        self.entries    = {}   # { pid: {bv, av, be, ae, el} }
        self._run_btn   = None
        self._build()

    # ── Build ────────────────────────────────────────────────────────────────

    def _build(self):
        SectionHeader(self, "①", "PROCESS QUEUE").pack(
            fill="x", padx=18, pady=(16, 8))
        self._build_column_headers()
        for pid in Process.VALID_IDS:
            self._build_process_row(pid)
        tk.Frame(self, bg=AppTheme.BORDER, height=1).pack(
            fill="x", padx=14, pady=12)
        self._build_buttons()

    def _build_column_headers(self):
        row = tk.Frame(self, bg=AppTheme.SURFACE)
        row.pack(fill="x", padx=14, pady=(0, 6))
        for txt, w in [
            ("Process", 8),
            ("CPU Burst Time (msec)", 22),
            ("Arrival Time (msec)", 20),
        ]:
            tk.Label(row, text=txt, bg=AppTheme.SURFACE, fg=AppTheme.MUTED,
                font=("Consolas", 9, "bold"),
                width=w, anchor="w").pack(side="left", padx=8, pady=9)

    def _build_process_row(self, pid: str):
        row = tk.Frame(self, bg=AppTheme.CARD)
        row.pack(fill="x", padx=14, pady=5)

        # Colored dot + PID badge
        badge = tk.Frame(row, bg=AppTheme.CARD)
        badge.pack(side="left", padx=6)
        cv = tk.Canvas(badge, width=12, height=12,
            bg=AppTheme.CARD, highlightthickness=0)
        cv.pack(side="left", padx=(0, 6))
        cv.create_oval(1, 1, 12, 12, fill=AppTheme.PROC_COLORS[pid], outline="")
        tk.Label(badge, text=pid, bg=AppTheme.CARD,
            fg=AppTheme.PROC_COLORS[pid],
            font=("Consolas", 10, "bold"), width=3).pack(side="left")

        bv, av = tk.StringVar(), tk.StringVar()

        be = tk.Entry(row, textvariable=bv, bg=AppTheme.SURFACE, fg=AppTheme.TEXT,
            insertbackground=AppTheme.ACCENT, relief="flat",
            font=AppTheme.FONT_MONO, width=14,
            highlightbackground=AppTheme.BORDER, highlightthickness=1)
        be.pack(side="left", padx=(10, 0), ipady=7)

        ae = tk.Entry(row, textvariable=av, bg=AppTheme.SURFACE, fg=AppTheme.TEXT,
            insertbackground=AppTheme.ACCENT, relief="flat",
            font=AppTheme.FONT_MONO, width=14,
            highlightbackground=AppTheme.BORDER, highlightthickness=1)
        ae.pack(side="left", padx=(18, 0), ipady=7)

        el = tk.Label(row, text="", bg=AppTheme.CARD, fg=AppTheme.DANGER,
            font=("Consolas", 8))
        el.pack(side="left", padx=(10, 0))

        self.entries[pid] = dict(bv=bv, av=av, be=be, ae=ae, el=el)

    def _build_buttons(self):
        btn_row = tk.Frame(self, bg=AppTheme.CARD)
        btn_row.pack(anchor="w", padx=18, pady=(0, 18))

        self._run_btn = HoverButton(btn_row,
            text="▶  Run FCFS",
            bg=AppTheme.ACCENT, fg="#0d1117",
            font=("Consolas", 10, "bold"),
            padx=22, pady=9,
            hover_bg="#79b8ff", hover_fg="#0d1117",
            command=self._on_run)
        self._run_btn.pack(side="left", padx=(0, 12))

        HoverButton(btn_row, text="↺  Reset",
            bg=AppTheme.CARD, fg=AppTheme.MUTED,
            font=AppTheme.FONT_MONO, padx=18, pady=9,
            hover_fg=AppTheme.TEXT,
            command=self._on_reset).pack(side="left")

    # ── Public API ───────────────────────────────────────────────────────────

    def validate(self) -> list[Process] | None:
        """
        Reads all entries.  Empty rows are skipped.
        Returns None if any error is found, else a list of Process objects.
        """
        processes, has_error = [], False
        for pid in Process.VALID_IDS:
            e = self.entries[pid]
            bs  = e["bv"].get().strip()
            as_ = e["av"].get().strip()
            e["be"].config(highlightbackground=AppTheme.BORDER)
            e["ae"].config(highlightbackground=AppTheme.BORDER)
            e["el"].config(text="")

            if bs == "" and as_ == "":
                continue

            err, b, a = "", None, None

            if bs == "":
                err = "Burst required"
                e["be"].config(highlightbackground=AppTheme.DANGER)
            else:
                try:
                    b = int(bs)
                    if b < 1:
                        raise ValueError
                except ValueError:
                    err = "Burst ≥ 1"
                    e["be"].config(highlightbackground=AppTheme.DANGER)

            if as_ == "":
                err += ("  " if err else "") + "Arrival required"
                e["ae"].config(highlightbackground=AppTheme.DANGER)
            else:
                try:
                    a = int(as_)
                    if a < 0:
                        raise ValueError
                except ValueError:
                    err += ("  " if err else "") + "Arrival ≥ 0"
                    e["ae"].config(highlightbackground=AppTheme.DANGER)

            if err:
                e["el"].config(text=err)
                has_error = True
            elif b is not None and a is not None:
                processes.append(Process(pid, b, a))

        return None if has_error else processes

    def reset(self):
        for pid in Process.VALID_IDS:
            e = self.entries[pid]
            e["bv"].set("")
            e["av"].set("")
            e["be"].config(highlightbackground=AppTheme.BORDER)
            e["ae"].config(highlightbackground=AppTheme.BORDER)
            e["el"].config(text="")
        self.set_run_button_state(normal=True)

    def set_run_button_state(self, normal: bool):
        if normal:
            self._run_btn.config(
                state="normal", text="▶  Run FCFS", bg=AppTheme.ACCENT)
        else:
            self._run_btn.config(
                state="disabled", text="⏳ Scheduling...", bg=AppTheme.MUTED)

    def flash_rows(self, processes: list[Process], callback):
        """Flash each active process row's entry borders, then call callback."""
        pid_set = {p.pid for p in processes}
        pids    = [p for p in Process.VALID_IDS if p in pid_set]

        def flash(idx, on):
            if idx >= len(pids):
                self.after(200, callback)
                return
            pid   = pids[idx]
            color = AppTheme.PROC_COLORS[pid] if on else AppTheme.BORDER
            self.entries[pid]["be"].config(highlightbackground=color)
            self.entries[pid]["ae"].config(highlightbackground=color)
            if on:
                self.after(180, lambda: flash(idx, False))
            else:
                self.after(60,  lambda: flash(idx + 1, True))

        flash(0, True)
