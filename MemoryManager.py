class MemoryManager:
    """
    Dynamic memory allocation model.
    Divides available RAM equally among scheduled processes.
    """

    TOTAL_MB = 64
    OS_MB    = 8

    def allocate(self, processes: list[Process]) -> list[dict]:
        """
        Returns a list of memory segment dicts:
            { label, mb, color, is_process }
        Always starts with the OS segment and ends with a
        Free segment if there is leftover memory.
        """
        available = self.TOTAL_MB - self.OS_MB
        per_proc  = math.floor(available / len(processes))
        free_mb   = available - per_proc * len(processes)

        segments = [{"label": "OS Kernel", "mb": self.OS_MB,
                     "color": "#2d333b", "is_process": False}]
        for p in processes:
            segments.append({
                "label":      p.pid,
                "mb":         per_proc,
                "color":      AppTheme.PROC_COLORS[p.pid],
                "is_process": True,
            })
        if free_mb > 0:
            segments.append({"label": "Free", "mb": free_mb,
                              "color": "#1c2333", "is_process": False})
        return segments
