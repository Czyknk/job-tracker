-- Recent activity on the dashboard reads the newest history entries across all applications
CREATE INDEX idx_status_history_changed_at ON status_history (changed_at DESC, id DESC);
