CREATE TABLE rides (
   id VARCHAR(36) PRIMARY KEY,
	rider_id VARCHAR(255) NOT NULL,
	driver_id VARCHAR(255),
	pickup_latitude DOUBLE NOT NULL,
	pickup_longitude DOUBLE NOT NULL,
	pickup_address VARCHAR(500) NOT NULL,
	drop_latitude DOUBLE NOT NULL,
	drop_longitude DOUBLE NOT NULL,
	drop_address VARCHAR(500) NOT NULL,
	status VARCHAR(50) NOT NULL,
	estimated_fare DOUBLE,
	actual_fare DOUBLE,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	started_at TIMESTAMP,
	completed_at TIMESTAMP
);

-- Create indexes for common query patterns
CREATE INDEX idx_rides_rider_id ON rides(rider_id);
CREATE INDEX idx_rides_driver_id ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
CREATE INDEX idx_rides_created_at ON rides(created_at);
