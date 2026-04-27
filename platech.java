"""
CPU Scheduler Simulator — First Come First Serve + Dynamic Memory Allocation
Members:
    Zabala, Kylle Luis L.
    Rodrigo, Ahron Daniel A.
    Resuelo, Hanna Gabrielle N.

Requirements: Python 3.x  (tkinter ships with standard Python)
Run: python cpu_scheduler_fcfs.py
"""

import tkinter as tk
from tkinter import ttk, messagebox
import math

# ── THEME ──────────────────────────────────────────────────────────────
# These are color constants used throughout the UI for a consistent dark theme
BG      = "#0d1117"   # main background (very dark)
SURFACE = "#161b22"   # slightly lighter background for panels
CARD    = "#1c2333"   # card/container background
BORDER  = "#30363d"   # border color for separating UI elements
TEXT    = "#e6edf3"   # main text color (near white)
MUTED   = "#8b949e"   # secondary/dimmed text
ACCENT  = "#58a6ff"   # highlight color (blue) for buttons and labels
SUCCESS = "#2ecc71"   # green — used for turnaround time
WARNING = "#f1c40f"   # yellow — used for waiting time
DANGER  = "#e74c3c"   # red — used for validation errors

# Each process is assigned a unique color for visual identification
PROC_COLORS = {
    "P1": "#c0392b",
    "P2": "#2980b9",
    "P3": "#4a5568",
    "P4": "#27ae60",
    "P5": "#d35400",
}

# Memory configuration constants
TOTAL_MEM_MB = 64   # total simulated RAM in MB
OS_MEM_MB    = 8    # memory reserved for the OS (always allocated first)
PROC_IDS     = ["P1", "P2", "P3", "P4", "P5"]  # fixed list of supported process IDs
MEM_H        = 340  # height (px) of the memory map canvas


# ── FCFS ALGORITHM ─────────────────────────────────────────────────────
def run_fcfs(processes):
    """
    Core scheduling algorithm — First Come First Serve (FCFS / FIFO).
    Processes are executed in the order they arrive, no preemption.
    """

    # Sort by arrival time; if two processes arrive at the same time,
    # break the tie using the fixed PROC_IDS order (P1 before P2, etc.)
    sorted_procs = sorted(processes,
        key=lambda p: (p["arrival"], PROC_IDS.index(p["id"])))

    t, results = 0, []  # t = current time on the CPU clock

    for p in sorted_procs:
        # If CPU is idle (no process arrived yet), jump the clock forward
        if t < p["arrival"]:
            t = p["arrival"]

        start = t                    # when this process starts on CPU
        ct    = t + p["burst"]       # Completion Time = start + burst
        tat   = ct - p["arrival"]    # Turnaround Time = CT - Arrival
        wt    = tat - p["burst"]     # Waiting Time = TAT - Burst

        # Store all computed values alongside the original process data
        results.append({**p, "start": start, "ct": ct, "tat": tat, "wt": wt})

        t = ct  # advance the clock to when this process finishes

    return results


# ── MAIN APPLICATION ───────────────────────────────────────────────────
class CPUSchedulerApp(tk.Tk):
    """
    Main application window. Inherits from tk.Tk so it IS the root window.
    Manages all screens: Welcome → Simulator.
    """

    def __init__(self):
        super().__init__()
        self.title("CPU Scheduler Simulator — FCFS")
        self.configure(bg=BG)
        self.resizable(True, True)
        self.minsize(1000, 680)
        self._mem_done_callback = None  # used to chain memory → gantt animation

        # Apply a dark custom theme to all ttk widgets (Treeview, Scrollbar, etc.)
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure(".",
            background=BG, foreground=TEXT,
            fieldbackground=CARD, bordercolor=BORDER,
            troughcolor=SURFACE, selectbackground=ACCENT,
            selectforeground="#0d1117", font=("Consolas", 10))
        style.configure("Treeview",
            background=CARD, foreground=TEXT,
            fieldbackground=CARD, rowheight=32,
            borderwidth=0, font=("Consolas", 10))
        style.configure("Treeview.Heading",
            background=SURFACE, foreground=MUTED,
            font=("Consolas", 9, "bold"), relief="flat", borderwidth=0)
        style.map("Treeview",
            background=[("selected", "#1f3a5f")],
            foreground=[("selected", ACCENT)])
        style.configure("TScrollbar",
            background=SURFACE, troughcolor=CARD,
            arrowcolor=MUTED, borderwidth=0)

        self._show_welcome()  # start with the welcome/splash screen

    # ────────────────────────────────────────────
    #  WELCOME
    # ────────────────────────────────────────────
    def _show_welcome(self):
        """Renders the splash/intro screen with member names and launch button."""
        self.geometry("720x600")
        self._clear()  # remove all existing widgets first

        frame = tk.Frame(self, bg=BG)
        frame.pack(fill="both", expand=True)

        tk.Label(frame, text="OPERATING SYSTEMS  ·  LAB PROJECT",
            bg=BG, fg=ACCENT, font=("Consolas", 9, "bold")).pack(pady=(56, 0))

        tk.Label(frame, text="CPU Scheduler Simulator",
            bg=BG, fg="#ffffff", font=("Consolas", 26, "bold")).pack(pady=(8, 2))

        tk.Label(frame,
            text="First Come First Serve  ·  Dynamic Memory Allocation",
            bg=BG, fg=ACCENT, font=("Consolas", 11)).pack(pady=(0, 32))

        # Card showing member names with their assigned process color dot
        mcard = tk.Frame(frame, bg=CARD,
            highlightbackground=BORDER, highlightthickness=1,
            padx=56, pady=24)
        mcard.pack()

        tk.Label(mcard, text="— PROJECT MEMBERS —",
            bg=CARD, fg=MUTED,
            font=("Consolas", 8, "bold")).pack(pady=(0, 14))

        # Draw a colored dot beside each member name using a small Canvas
        for pid, name in [
            ("P1", "Zabala, Kylle Luis L."),
            ("P2", "Rodrigo, Ahron Daniel A."),
            ("P3", "Resuelo, Hanna Gabrielle N."),
        ]:
            row = tk.Frame(mcard, bg=CARD)
            row.pack(anchor="w", pady=5)
            cv = tk.Canvas(row, width=12, height=12, bg=CARD, highlightthickness=0)
            cv.pack(side="left", padx=(0, 8))
            cv.create_oval(1, 1, 12, 12, fill=PROC_COLORS[pid], outline="")
            tk.Label(row, text=name, bg=CARD, fg=TEXT,
                font=("Consolas", 10)).pack(side="left")

        # Launch button transitions to the main simulator screen
        btn = tk.Button(frame,
            text="▶  LAUNCH SIMULATOR",
            bg=ACCENT, fg="#0d1117",
            font=("Consolas", 11, "bold"),
            relief="flat", padx=40, pady=14,
            cursor="hand2",
            command=self._show_simulator)
        btn.pack(pady=44)
        # Hover effect: slightly lighter blue on mouse-over
        btn.bind("<Enter>", lambda e: btn.config(bg="#79b8ff"))
        btn.bind("<Leave>", lambda e: btn.config(bg=ACCENT))

    # ────────────────────────────────────────────
    #  SIMULATOR SCREEN
    # ────────────────────────────────────────────
    def _show_simulator(self):
        """
        Builds the main simulator layout with 4 sections:
          ① Process Queue (input)
          ② Memory Map (visual)
          ③ Gantt Chart
          ④ Process Statistics table
        """
        self.geometry("1200x820")
        self._clear()

        # ── Header bar at the top ──────────────────
        hdr = tk.Frame(self, bg=SURFACE,
            highlightbackground=BORDER, highlightthickness=1)
        hdr.pack(fill="x")

        tk.Label(hdr, text="CPU Scheduler Simulator",
            bg=SURFACE, fg="#fff",
            font=("Consolas", 14, "bold")).pack(side="left", padx=20, pady=12)
        tk.Label(hdr, text="FCFS  ·  Dynamic Memory Allocation",
            bg=SURFACE, fg=ACCENT,
            font=("Consolas", 9)).pack(side="left", pady=12)

        back = tk.Button(hdr, text="← Back",
            bg=SURFACE, fg=MUTED,
            font=("Consolas", 9), relief="flat",
            padx=14, pady=8, cursor="hand2",
            command=self._show_welcome)
        back.pack(side="right", padx=20, pady=10)
        back.bind("<Enter>", lambda e: back.config(fg=ACCENT))
        back.bind("<Leave>", lambda e: back.config(fg=MUTED))

        # ── Scrollable main area (for smaller screens) ──
        self._outer_canvas = tk.Canvas(self, bg=BG, highlightthickness=0)
        sb = ttk.Scrollbar(self, orient="vertical",
            command=self._outer_canvas.yview)
        self._outer_canvas.configure(yscrollcommand=sb.set)
        sb.pack(side="right", fill="y")
        self._outer_canvas.pack(fill="both", expand=True)

        # A regular Frame placed inside the Canvas acts as the scrollable content
        self._main_frame = tk.Frame(self._outer_canvas, bg=BG)
        self._outer_canvas.create_window((0, 0), window=self._main_frame, anchor="nw")

        # Update scroll region whenever the inner frame changes size
        self._main_frame.bind("<Configure>", lambda e:
            self._outer_canvas.configure(
                scrollregion=self._outer_canvas.bbox("all")))

        # Allow mouse wheel scrolling on all platforms
        self._outer_canvas.bind_all("<MouseWheel>", lambda e:
            self._outer_canvas.yview_scroll(int(-1*(e.delta/120)), "units"))

        # ── Top row: Process Queue (left) | Memory Map (right) ──
        top_row = tk.Frame(self._main_frame, bg=BG)
        top_row.pack(fill="x", padx=24, pady=(20, 0))

        self._input_col = tk.Frame(top_row, bg=BG)
        self._input_col.pack(side="left", fill="both", expand=True, padx=(0, 16))

        self._mem_col = tk.Frame(top_row, bg=BG)
        self._mem_col.pack(side="left", fill="y", anchor="n")

        # ── Bottom row: Gantt Chart + Stats (full width) ──
        self._bottom_col = tk.Frame(self._main_frame, bg=BG)
        self._bottom_col.pack(fill="x", padx=24, pady=(16, 24))

        # Build each section
        self._build_input_section()
        self._build_memory_panel()
        self._build_gantt_section()
        self._build_stats_section()

    # ────────────────────────────────────────────
    #  ① PROCESS QUEUE (Input Form)
    # ────────────────────────────────────────────
    def _build_input_section(self):
        """
        Creates one input row per process (P1–P5).
        Each row has: process label, burst time field, arrival time field, error label.
        """
        card = tk.Frame(self._input_col, bg=CARD,
            highlightbackground=BORDER, highlightthickness=1)
        card.pack(fill="x")

        sec_hdr = tk.Frame(card, bg=CARD)
        sec_hdr.pack(fill="x", padx=18, pady=(16, 8))
        tk.Label(sec_hdr, text="①", bg=CARD, fg=ACCENT,
            font=("Consolas", 10, "bold")).pack(side="left", padx=(0, 8))
        tk.Label(sec_hdr, text="PROCESS QUEUE",
            bg=CARD, fg=MUTED,
            font=("Consolas", 8, "bold")).pack(side="left")

        # Column headers (labels above the input fields)
        col_hdr = tk.Frame(card, bg=SURFACE)
        col_hdr.pack(fill="x", padx=14, pady=(0, 6))
        for txt, w in [("Process", 8), ("CPU Burst Time (msec)", 22), ("Arrival Time (msec)", 20)]:
            tk.Label(col_hdr, text=txt, bg=SURFACE, fg=MUTED,
                font=("Consolas", 9, "bold"),
                width=w, anchor="w").pack(side="left", padx=8, pady=9)

        # self.entries stores references to each process's input widgets
        self.entries = {}
        for pid in PROC_IDS:
            row = tk.Frame(card, bg=CARD)
            row.pack(fill="x", padx=14, pady=5)

            # Colored dot + process ID badge on the left
            badge = tk.Frame(row, bg=CARD)
            badge.pack(side="left", padx=6)
            cv = tk.Canvas(badge, width=12, height=12,
                bg=CARD, highlightthickness=0)
            cv.pack(side="left", padx=(0, 6))
            cv.create_oval(1, 1, 12, 12, fill=PROC_COLORS[pid], outline="")
            tk.Label(badge, text=pid, bg=CARD, fg=PROC_COLORS[pid],
                font=("Consolas", 10, "bold"), width=3).pack(side="left")

            # StringVar lets us get/set the entry value programmatically
            bv, av = tk.StringVar(), tk.StringVar()

            # Burst time input field
            be = tk.Entry(row, textvariable=bv, bg=SURFACE, fg=TEXT,
                insertbackground=ACCENT, relief="flat",
                font=("Consolas", 10), width=14,
                highlightbackground=BORDER, highlightthickness=1)
            be.pack(side="left", padx=(10, 0), ipady=7)

            # Arrival time input field
            ae = tk.Entry(row, textvariable=av, bg=SURFACE, fg=TEXT,
                insertbackground=ACCENT, relief="flat",
                font=("Consolas", 10), width=14,
                highlightbackground=BORDER, highlightthickness=1)
            ae.pack(side="left", padx=(18, 0), ipady=7)

            # Inline error label shown when validation fails
            el = tk.Label(row, text="", bg=CARD, fg=DANGER,
                font=("Consolas", 8))
            el.pack(side="left", padx=(10, 0))

            # Store all widget references for this process
            self.entries[pid] = dict(bv=bv, av=av, be=be, ae=ae, el=el)

        tk.Frame(card, bg=BORDER, height=1).pack(fill="x", padx=14, pady=12)

        # Action buttons: Run FCFS and Reset
        btn_row = tk.Frame(card, bg=CARD)
        btn_row.pack(anchor="w", padx=18, pady=(0, 18))

        self._run_btn = tk.Button(btn_row,
            text="▶  Run FCFS",
            bg=ACCENT, fg="#0d1117",
            font=("Consolas", 10, "bold"),
            relief="flat", padx=22, pady=9,
            cursor="hand2",
            command=self._on_run)
        self._run_btn.pack(side="left", padx=(0, 12))
        self._run_btn.bind("<Enter>", lambda e: self._run_btn.config(bg="#79b8ff"))
        self._run_btn.bind("<Leave>", lambda e: self._run_btn.config(bg=ACCENT))

        rst = tk.Button(btn_row, text="↺  Reset",
            bg=CARD, fg=MUTED,
            font=("Consolas", 10), relief="flat",
            padx=18, pady=9, cursor="hand2",
            command=self._reset)
        rst.pack(side="left")
        rst.bind("<Enter>", lambda e: rst.config(fg=TEXT))
        rst.bind("<Leave>", lambda e: rst.config(fg=MUTED))

    # ────────────────────────────────────────────
    #  ② MEMORY MAP
    # ────────────────────────────────────────────
    def _build_memory_panel(self):
        """Sets up the Memory Map canvas area (right side of top row)."""
        card = tk.Frame(self._mem_col, bg=CARD,
            highlightbackground=BORDER, highlightthickness=1)
        card.pack(fill="y")

        sec_hdr = tk.Frame(card, bg=CARD)
        sec_hdr.pack(fill="x", padx=18, pady=(16, 8))
        tk.Label(sec_hdr, text="②", bg=CARD, fg=ACCENT,
            font=("Consolas", 10, "bold")).pack(side="left", padx=(0, 8))
        tk.Label(sec_hdr, text="MEMORY MAP (MB)",
            bg=CARD, fg=MUTED,
            font=("Consolas", 8, "bold")).pack(side="left")

        # Canvas where memory blocks will be drawn
        self._mem_canvas = tk.Canvas(card,
            bg=SURFACE, width=210, height=MEM_H,
            highlightthickness=0)
        self._mem_canvas.pack(padx=14, pady=(0, 10))

        # Show idle placeholder shortly after the widget renders
        self._mem_canvas.after(50, self._draw_mem_idle)

        tk.Frame(card, bg=BORDER, height=1).pack(fill="x", padx=14, pady=(0, 10))

        # Legend area below the canvas (shows color-to-process mapping)
        self._mem_legend = tk.Frame(card, bg=CARD)
        self._mem_legend.pack(fill="x", padx=14, pady=(0, 16))

    def _draw_mem_idle(self):
        """Shows a placeholder on the memory canvas before any process runs."""
        c = self._mem_canvas
        c.delete("all")
        w = c.winfo_width() or 210
        c.create_rectangle(0, 0, w, MEM_H, fill=SURFACE, outline="")
        c.create_text(w // 2, MEM_H // 2,
            text="Awaiting\nprocesses...",
            fill=MUTED, font=("Consolas", 9), justify="center")

    def _draw_memory(self, results):
        """
        Dynamic Memory Allocation visualization.

        Strategy: divide usable RAM equally among scheduled processes.
          - OS always gets the first 8 MB (reserved)
          - Remaining 56 MB is split equally among processes
          - Any leftover bytes are shown as 'Free'

        Segments are animated top-to-bottom, one block at a time.
        """
        # Clear old legend entries
        for w in self._mem_legend.winfo_children():
            w.destroy()

        # Equal allocation: floor divide usable memory by number of processes
        per_proc = math.floor((TOTAL_MEM_MB - OS_MEM_MB) / len(results))
        # Leftover after integer division (internal fragmentation)
        free_mb  = TOTAL_MEM_MB - OS_MEM_MB - per_proc * len(results)

        # Build segment list: OS first, then each process, then Free (if any)
        segs = [{"label": "OS Kernel", "mb": OS_MEM_MB, "color": "#2d333b", "pid": False}]
        for p in results:
            segs.append({"label": p["id"], "mb": per_proc,
                         "color": PROC_COLORS[p["id"]], "pid": True})
        if free_mb > 0:
            segs.append({"label": "Free", "mb": free_mb, "color": "#1c2333", "pid": False})

        c = self._mem_canvas
        c.delete("all")
        h, w = MEM_H, (c.winfo_width() or 210)

        # Pre-calculate y-positions: height of each block is proportional to its MB size
        positions = []
        y = 0
        for seg in segs:
            seg_h = max(int((seg["mb"] / TOTAL_MEM_MB) * h), 22)  # min 22px so label fits
            positions.append((seg, y, seg_h))
            y += seg_h

        def draw_up_to(n):
            """Redraw all segments revealed so far (up to index n)."""
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
            # Update legend to match revealed segments
            for w2 in self._mem_legend.winfo_children():
                w2.destroy()
            for i, (seg, _, _) in enumerate(positions):
                if i >= n or not seg["pid"]:
                    continue
                row = tk.Frame(self._mem_legend, bg=CARD)
                row.pack(anchor="w", pady=2)
                cv2 = tk.Canvas(row, width=10, height=10,
                    bg=CARD, highlightthickness=0)
                cv2.pack(side="left", padx=(0, 5))
                cv2.create_rectangle(0, 0, 10, 10, fill=seg["color"], outline="")
                tk.Label(row, text=f"{seg['label']} — {seg['mb']} MB",
                    bg=CARD, fg=MUTED,
                    font=("Consolas", 8)).pack(side="left")

        def animate_mem(n=0):
            """Recursively reveal one memory block every 110ms."""
            draw_up_to(n + 1)
            if n + 1 < len(positions):
                self.after(110, lambda: animate_mem(n + 1))
            elif self._mem_done_callback:
                # All blocks drawn → trigger next animation (Gantt Chart)
                self.after(200, self._mem_done_callback)

        animate_mem(0)

    # ────────────────────────────────────────────
    #  ③ GANTT CHART
    # ────────────────────────────────────────────
    def _build_gantt_section(self):
        """
        Sets up two canvases:
          - _gantt_canvas: the colored process blocks
          - _time_canvas: the time axis below the blocks
        """
        card = tk.Frame(self._bottom_col, bg=CARD,
            highlightbackground=BORDER, highlightthickness=1)
        card.pack(fill="x", pady=(0, 14))

        sec_hdr = tk.Frame(card, bg=CARD)
        sec_hdr.pack(fill="x", padx=18, pady=(16, 8))
        tk.Label(sec_hdr, text="③", bg=CARD, fg=ACCENT,
            font=("Consolas", 10, "bold")).pack(side="left", padx=(0, 8))
        tk.Label(sec_hdr, text="GANTT CHART",
            bg=CARD, fg=MUTED,
            font=("Consolas", 8, "bold")).pack(side="left")

        # Main bar area for process blocks
        self._gantt_canvas = tk.Canvas(card,
            bg=SURFACE, height=68,
            highlightthickness=0)
        self._gantt_canvas.pack(fill="x", padx=18, pady=(0, 4))

        # Thinner canvas below for tick marks and time labels
        self._time_canvas = tk.Canvas(card,
            bg=CARD, height=26,
            highlightthickness=0)
        self._time_canvas.pack(fill="x", padx=18, pady=(0, 16))

        # Show a placeholder until the user runs FCFS
        self._gantt_canvas.after(60, lambda:
            self._draw_placeholder(self._gantt_canvas, 68,
                "Run FCFS to see the Gantt chart"))

    # ────────────────────────────────────────────
    #  ④ PROCESS STATISTICS
    # ────────────────────────────────────────────
    def _build_stats_section(self):
        """
        Creates the results table (Treeview) and three average metric boxes:
          - Average Waiting Time
          - Average Turnaround Time
          - Average Completion Time
        """
        card = tk.Frame(self._bottom_col, bg=CARD,
            highlightbackground=BORDER, highlightthickness=1)
        card.pack(fill="x")

        sec_hdr = tk.Frame(card, bg=CARD)
        sec_hdr.pack(fill="x", padx=18, pady=(16, 8))
        tk.Label(sec_hdr, text="④", bg=CARD, fg=ACCENT,
            font=("Consolas", 10, "bold")).pack(side="left", padx=(0, 8))
        tk.Label(sec_hdr, text="PROCESS STATISTICS  —  WT · CT · TAT",
            bg=CARD, fg=MUTED,
            font=("Consolas", 8, "bold")).pack(side="left")

        tree_wrap = tk.Frame(card, bg=CARD)
        tree_wrap.pack(fill="x", padx=18, pady=(0, 12))

        # ttk.Treeview is used as a table; each column maps to a schedule metric
        cols = ("Process", "Arrival (ms)", "Burst (ms)",
                "Completion (ms)", "Waiting (ms)", "Turnaround (ms)")
        self._tree = ttk.Treeview(tree_wrap, columns=cols,
            show="headings", height=5)
        for col, cw in zip(cols, [100, 120, 110, 140, 120, 145]):
            self._tree.heading(col, text=col)
            self._tree.column(col, width=cw, anchor="center")
        self._tree.pack(fill="x")

        # Pre-populate all rows with "—" placeholders (filled after scheduling)
        for pid in PROC_IDS:
            self._tree.insert("", "end",
                values=(pid, "—", "—", "—", "—", "—"),
                tags=(pid.lower(),))
            self._tree.tag_configure(pid.lower(), foreground=PROC_COLORS[pid])

        tk.Frame(card, bg=BORDER, height=1).pack(fill="x", padx=18, pady=(4, 14))

        # Average metric boxes at the bottom of the stats card
        avg_row = tk.Frame(card, bg=CARD)
        avg_row.pack(fill="x", padx=18, pady=(0, 20))

        self._avg_labels = {}
        for key, label, color in [
            ("wt",  "Avg Waiting Time",    WARNING),
            ("tat", "Avg Turnaround Time", SUCCESS),
            ("ct",  "Avg Completion Time", ACCENT),
        ]:
            box = tk.Frame(avg_row, bg=SURFACE,
                highlightbackground=BORDER, highlightthickness=1,
                padx=18, pady=14)
            box.pack(side="left", expand=True, fill="x", padx=(0, 10))
            tk.Label(box, text=label.upper(),
                bg=SURFACE, fg=MUTED,
                font=("Consolas", 7, "bold")).pack(anchor="w")
            lbl = tk.Label(box, text="— ms",
                bg=SURFACE, fg=color,
                font=("Consolas", 16, "bold"))
            lbl.pack(anchor="w", pady=(6, 0))
            self._avg_labels[key] = lbl  # save reference so we can update it later

    # ────────────────────────────────────────────
    #  VALIDATION
    # ────────────────────────────────────────────
    def _validate(self):
        """
        Reads and validates all input fields.
        Rules:
          - A row is skipped if both fields are empty (process not used)
          - Burst time must be an integer ≥ 1
          - Arrival time must be an integer ≥ 0
        Returns a list of valid process dicts, or None if any error found.
        """
        inputs, has_error = [], False
        for pid in PROC_IDS:
            e = self.entries[pid]
            bs  = e["bv"].get().strip()   # burst input string
            as_ = e["av"].get().strip()   # arrival input string

            # Reset border colors and clear old error messages
            e["be"].config(highlightbackground=BORDER)
            e["ae"].config(highlightbackground=BORDER)
            e["el"].config(text="")

            # Skip empty rows (process not used in this run)
            if bs == "" and as_ == "":
                continue

            err, b, a = "", None, None

            # Validate burst time
            if bs == "":
                err = "Burst required"
                e["be"].config(highlightbackground=DANGER)
            else:
                try:
                    b = int(bs)
                    if b < 1: raise ValueError
                except ValueError:
                    err = "Burst ≥ 1"
                    e["be"].config(highlightbackground=DANGER)

            # Validate arrival time
            if as_ == "":
                err += ("  " if err else "") + "Arrival required"
                e["ae"].config(highlightbackground=DANGER)
            else:
                try:
                    a = int(as_)
                    if a < 0: raise ValueError
                except ValueError:
                    err += ("  " if err else "") + "Arrival ≥ 0"
                    e["ae"].config(highlightbackground=DANGER)

            if err:
                e["el"].config(text=err)
                has_error = True
            elif b is not None and a is not None:
                inputs.append({"id": pid, "burst": b, "arrival": a})

        # Return None to signal validation failed (caller should not proceed)
        return None if has_error else inputs

    # ────────────────────────────────────────────
    #  RESET
    # ────────────────────────────────────────────
    def _reset(self):
        """Clears all inputs and resets all output sections to their blank state."""
        # Clear input fields and errors
        for pid in PROC_IDS:
            e = self.entries[pid]
            e["bv"].set("")
            e["av"].set("")
            e["be"].config(highlightbackground=BORDER)
            e["ae"].config(highlightbackground=BORDER)
            e["el"].config(text="")

        # Reset Gantt Chart to placeholder
        self._gantt_canvas.delete("all")
        self._time_canvas.delete("all")
        self._draw_placeholder(self._gantt_canvas, 68, "Run FCFS to see the Gantt chart")

        # Reset statistics table to "—" placeholders
        for row in self._tree.get_children():
            self._tree.delete(row)
        for pid in PROC_IDS:
            self._tree.insert("", "end",
                values=(pid, "—", "—", "—", "—", "—"),
                tags=(pid.lower(),))
            self._tree.tag_configure(pid.lower(), foreground=PROC_COLORS[pid])

        # Reset average metric labels
        for key in self._avg_labels:
            self._avg_labels[key].config(text="— ms")

        # Reset memory map
        self._draw_mem_idle()
        for w in self._mem_legend.winfo_children():
            w.destroy()

        # Re-enable the Run button
        self._run_btn.config(state="normal", text="▶  Run FCFS", bg=ACCENT)

    # ────────────────────────────────────────────
    #  RUN — Triggered when user clicks "Run FCFS"
    #
    #  Animation order (chained, not simultaneous):
    #  ① Flash input rows  →  ② Memory Map  →  ③ Gantt  →  ④ Stats
    # ────────────────────────────────────────────
    def _on_run(self):
        """
        Entry point for running the simulation.
        1. Validate inputs
        2. Run FCFS algorithm
        3. Start the chained animation sequence
        """
        inputs = self._validate()
        if inputs is None:
            messagebox.showerror("Validation Error",
                "Please fix the highlighted fields.")
            return
        if not inputs:
            messagebox.showwarning("No Input",
                "Enter at least one process.")
            return

        # Run the FCFS algorithm; store results for animation steps
        self._results = run_fcfs(inputs)

        # Disable button while animations play to prevent duplicate runs
        self._run_btn.config(state="disabled", text="⏳ Scheduling...", bg=MUTED)

        # Start animation chain: ① flash rows first
        self._flash_input_rows(inputs,
            callback=self._start_memory_animation)

    def _start_memory_animation(self):
        """Step ②: begin Memory Map animation; when done, go to Gantt."""
        self._mem_done_callback = self._start_gantt_animation
        self._draw_memory(self._results)

    def _start_gantt_animation(self):
        """Step ③: begin Gantt Chart animation; when done, show Stats."""
        self._animate_gantt(self._results,
            callback=lambda: self._animate_stats(self._results, callback=None))

    # ────────────────────────────────────────────
    #  ANIMATION ①: Flash Process Queue rows
    # ────────────────────────────────────────────
    def _flash_input_rows(self, inputs, callback):
        """
        Briefly highlights each active input row with its process color
        to confirm which processes will be scheduled.
        Flashes one process at a time, then calls callback when done.
        """
        pid_set = {p["id"] for p in inputs}
        pids = [p for p in PROC_IDS if p in pid_set]

        def flash(idx, on):
            if idx >= len(pids):
                self.after(200, callback)  # all done → next step
                return
            pid = pids[idx]
            e = self.entries[pid]
            color = PROC_COLORS[pid] if on else BORDER  # toggle highlight on/off
            e["be"].config(highlightbackground=color)
            e["ae"].config(highlightbackground=color)
            if on:
                self.after(180, lambda: flash(idx, False))    # keep lit for 180ms
            else:
                self.after(60, lambda: flash(idx + 1, True))  # move to next process

        flash(0, True)

    # ────────────────────────────────────────────
    #  ANIMATION ③: Gantt Chart blocks
    # ────────────────────────────────────────────
    def _animate_gantt(self, results, callback):
        """
        Draws the Gantt chart block by block (left to right).
        - IDLE gaps are shown as dark blocks when the CPU is waiting for arrivals
        - Process blocks grow from left to right (grow animation)
        - Time axis is drawn after all blocks are placed
        """
        self._gantt_canvas.delete("all")
        self._time_canvas.delete("all")

        self._gantt_canvas.update_idletasks()
        bar_w = max(self._gantt_canvas.winfo_width(), 600)

        end_time   = results[-1]["ct"]
        first_time = results[0]["start"] if results[0]["start"] > 0 else 0
        total_span = max(end_time - first_time, 1)  # total time range (avoids div-by-zero)

        segments = []
        prev_end = first_time

        # If first process doesn't arrive at t=0, draw an IDLE block at the start
        if results[0]["start"] > 0:
            segments.append({"kind": "idle",
                "w": max(int((results[0]["start"] / total_span) * bar_w), 10)})
            prev_end = results[0]["start"]

        # Build segment list; insert IDLE gaps between processes if needed
        for p in results:
            if p["start"] > prev_end:
                gap = p["start"] - prev_end
                segments.append({"kind": "idle",
                    "w": max(int((gap / total_span) * bar_w), 10)})
            # Block width is proportional to burst time relative to total span
            blk_w = max(int((p["burst"] / total_span) * bar_w), 32)
            segments.append({"kind": "proc", "proc": p, "w": blk_w})
            prev_end = p["ct"]

        # Pre-calculate x positions for each segment
        x = 0
        for seg in segments:
            seg["x"] = x
            x += seg["w"]

        def draw_segment(idx):
            """Draw one segment at a time using recursion + self.after() delays."""
            if idx >= len(segments):
                # All segments drawn → draw the time axis and re-enable button
                self._draw_time_axis(results, total_span, first_time, bar_w)
                self._run_btn.config(state="normal", text="▶  Run FCFS", bg=ACCENT)
                if callback:
                    callback()
                return
            seg = segments[idx]
            sx, sw = seg["x"], seg["w"]
            if seg["kind"] == "idle":
                # IDLE blocks appear instantly (no grow animation)
                self._gantt_canvas.create_rectangle(
                    sx, 0, sx + sw, 68, fill="#2d333b", outline="#0d1117")
                self._gantt_canvas.create_text(
                    sx + sw // 2, 34, text="IDLE",
                    fill=MUTED, font=("Consolas", 8))
                self.after(80, lambda: draw_segment(idx + 1))
            else:
                # Process blocks use the grow animation before moving to the next
                p = seg["proc"]
                self._animate_block_grow(sx, sw, PROC_COLORS[p["id"]], p,
                    done_cb=lambda: self.after(60, lambda: draw_segment(idx + 1)))

        draw_segment(0)

    def _animate_block_grow(self, sx, sw, color, proc, done_cb, step=0):
        """
        Grows a Gantt block from 0 to full width over STEPS frames.
        Each call redraws the block slightly wider using self.after() for timing.
        """
        STEPS = 12  # number of frames in the grow animation
        current_w = int(sw * (step + 1) / STEPS)  # width grows each step
        self._gantt_canvas.delete(f"grow_{sx}")    # remove previous frame's block
        self._gantt_canvas.create_rectangle(
            sx, 0, sx + current_w, 68,
            fill=color, outline="#0d1117", width=1,
            tags=f"grow_{sx}")  # tag lets us delete just this block next frame
        if step + 1 >= STEPS:
            # Final frame: add process ID and burst time text labels
            self._gantt_canvas.create_text(
                sx + sw // 2, 24, text=proc["id"],
                fill="#fff", font=("Consolas", 9, "bold"))
            self._gantt_canvas.create_text(
                sx + sw // 2, 46, text=f"{proc['burst']}ms",
                fill="white", font=("Consolas", 7))
            done_cb()  # signal that this block is complete
        else:
            self.after(18, lambda: self._animate_block_grow(
                sx, sw, color, proc, done_cb, step + 1))

    def _draw_time_axis(self, results, total_span, first_time, bar_w):
        """
        Draws tick marks and time labels below the Gantt chart.
        Ticks are placed at each process start and completion time.
        Overlapping ticks are skipped to keep the axis readable.
        """
        ticks = set()
        for p in results:
            ticks.add(p["start"])
            ticks.add(p["ct"])
        if results[0]["start"] == 0:
            ticks.add(0)

        prev_px = -30  # track last tick position to avoid crowding
        for t in sorted(ticks):
            px = int((t - first_time) / total_span * bar_w)
            if px - prev_px > 18:  # only draw if spaced at least 18px from last
                self._time_canvas.create_line(px, 0, px, 6, fill=BORDER)
                self._time_canvas.create_text(px, 17,
                    text=str(t), fill=MUTED, font=("Consolas", 7))
                prev_px = px

    # ────────────────────────────────────────────
    #  ANIMATION ④: Stats table rows + averages
    # ────────────────────────────────────────────
    def _animate_stats(self, results, callback):
        """
        Inserts result rows into the statistics table one by one with a 130ms delay.
        Processes not scheduled are shown with "—" placeholders.
        After all rows are inserted, triggers the average counter animation.
        """
        # Clear existing rows
        for row in self._tree.get_children():
            self._tree.delete(row)

        # Insert "—" rows for processes that were not included in this run
        scheduled_ids = {p["id"] for p in results}
        for pid in PROC_IDS:
            if pid not in scheduled_ids:
                self._tree.insert("", "end",
                    values=(pid, "—", "—", "—", "—", "—"),
                    tags=(pid.lower(),))
                self._tree.tag_configure(pid.lower(), foreground=PROC_COLORS[pid])

        def insert_row(idx):
            """Recursively insert each result row with a 130ms gap between rows."""
            if idx >= len(results):
                self._animate_averages(results, callback)  # all rows done → show averages
                return
            p = results[idx]
            self._tree.insert("", "end",
                values=(p["id"], p["arrival"], p["burst"],
                        p["ct"], p["wt"], p["tat"]),
                tags=(p["id"].lower(),))
            self._tree.tag_configure(p["id"].lower(), foreground=PROC_COLORS[p["id"]])
            self.after(130, lambda: insert_row(idx + 1))

        insert_row(0)

    def _animate_averages(self, results, callback):
        """
        Counts up each average metric from 0 to the final value over 20 frames.
        Gives a 'counting up' effect instead of values appearing instantly.
        """
        n = len(results)
        target = {
            "wt":  round(sum(p["wt"]  for p in results) / n, 2),
            "tat": round(sum(p["tat"] for p in results) / n, 2),
            "ct":  round(sum(p["ct"]  for p in results) / n, 2),
        }
        STEPS = 20  # animation frames for the count-up effect

        def tick(step):
            frac = (step + 1) / STEPS  # fraction of final value to display
            for key, lbl in self._avg_labels.items():
                lbl.config(text=f"{round(target[key] * frac, 2)} ms")
            if step + 1 < STEPS:
                self.after(30, lambda: tick(step + 1))
            else:
                # Snap to exact final value on the last frame
                for key, lbl in self._avg_labels.items():
                    lbl.config(text=f"{target[key]} ms")
                if callback:
                    self.after(200, callback)

        tick(0)

    # ────────────────────────────────────────────
    #  UTILS
    # ────────────────────────────────────────────
    def _draw_placeholder(self, canvas, height, text):
        """Draws a centered text placeholder on any given canvas."""
        canvas.delete("all")
        canvas.update_idletasks()
        w = max(canvas.winfo_width(), 400)
        canvas.create_rectangle(0, 0, w, height, fill=SURFACE, outline="")
        canvas.create_text(w // 2, height // 2,
            text=text, fill=MUTED,
            font=("Consolas", 9), justify="center")

    def _clear(self):
        """Removes all widgets from the window (used when switching screens)."""
        for w in self.winfo_children():
            w.destroy()


# ── ENTRY POINT ────────────────────────────────────────────────────────
if __name__ == "__main__":
    # Only run the app if this script is executed directly (not imported)
    app = CPUSchedulerApp()
    app.mainloop()  # starts the tkinter event loop (keeps the window open)