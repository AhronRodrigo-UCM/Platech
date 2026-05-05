class Process:
    """Represents a single process with its scheduling attributes."""

    VALID_IDS = ["P1", "P2", "P3", "P4", "P5"]

    def __init__(self, pid: str, burst: int, arrival: int):
        if pid not in self.VALID_IDS:
            raise ValueError(f"Invalid process ID: {pid}")
        if burst < 1:
            raise ValueError("Burst time must be >= 1")
        if arrival < 0:
            raise ValueError("Arrival time must be >= 0")
        self.pid     = pid
        self.burst   = burst
        self.arrival = arrival
        # Computed after scheduling
        self.start   = 0
        self.ct      = 0   # completion time
        self.tat     = 0   # turnaround time
        self.wt      = 0   # waiting time

    def to_dict(self) -> dict:
        return {
            "id":      self.pid,
            "burst":   self.burst,
            "arrival": self.arrival,
            "start":   self.start,
            "ct":      self.ct,
            "tat":     self.tat,
            "wt":      self.wt,
        }
