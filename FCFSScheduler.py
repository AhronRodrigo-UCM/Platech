class FCFSScheduler:
    """
    Pure First-Come-First-Serve scheduling algorithm.
    No UI dependencies — easy to unit test in isolation.
    """

    def schedule(self, processes: list[Process]) -> list[Process]:
        """
        Sort by arrival time (VALID_IDS index breaks ties),
        then compute start, ct, tat, wt for each process.
        Returns the same list sorted and mutated in place.
        """
        processes.sort(key=lambda p: (p.arrival, Process.VALID_IDS.index(p.pid)))
        t = 0
        for p in processes:
            if t < p.arrival:
                t = p.arrival
            p.start = t
            p.ct    = t + p.burst
            p.tat   = p.ct - p.arrival
            p.wt    = p.tat - p.burst
            t = p.ct
        return processes