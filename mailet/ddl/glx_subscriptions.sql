-- glx_subscriptions table
CREATE TABLE glx_subscriptions(
  id SERIAL PRIMARY KEY,
  subscribed BOOLEAN DEFAULT TRUE,
  uuid VARCHAR(36) NOT NULL,
  email VARCHAR(125) NOT NULL,
  from_email VARCHAR(125) NULL,
  created_at TIMESTAMP DEFAULT NULL,
  unsubscribed_at TIMESTAMP DEFAULT NULL
);