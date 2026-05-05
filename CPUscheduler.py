class CPUSchedulerApp(tk.Tk):
    """
    Root Tk window.  Acts as a simple navigator between screens —
    each screen is a full-window Frame that replaces the previous one.
    """

    def __init__(self):
        super().__init__()
        self.title("CPU Scheduler Simulator — FCFS")
        self.configure(bg=AppTheme.BG)
        self.resizable(True, True)
        self.minsize(1000, 680)

        AppTheme.apply_ttk_style(ttk.Style(self))
        self._current_screen = None
        self.show_welcome()

    def _replace_screen(self, screen: tk.Frame, geometry: str):
        if self._current_screen:
            self._current_screen.destroy()
        self.geometry(geometry)
        screen.pack(fill="both", expand=True)
        self._current_screen = screen

    def show_welcome(self):
        self._replace_screen(
            WelcomeScreen(self, on_launch=self.show_simulator),
            "720x600")

    def show_simulator(self):
        self._replace_screen(
            SimulatorScreen(self, on_back=self.show_welcome),
            "1200x820")


# ── Entry point ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    app = CPUSchedulerApp()
    app.mainloop()
