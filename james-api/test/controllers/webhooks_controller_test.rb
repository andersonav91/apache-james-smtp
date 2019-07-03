require 'test_helper'

class WebhooksControllerTest < ActionDispatch::IntegrationTest
  test "should get sendgrid" do
    get webhooks_sendgrid_url
    assert_response :success
  end

end
