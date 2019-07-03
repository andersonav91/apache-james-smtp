class SubscriptionsController < ActionController::Base

  def unsubscribe
    subscription = GlxSubscription.where(uuid: params[:uuid]).first
    if subscription.nil?
      @message = "Invalid code, please verify that the code entered in the url or try later."
    else
      subscription.unsubscribed_at = Time.now
      subscription.subscribed = false
      subscription.save
      @message = "You have been succesfully removed from the mailing list."
    end
  end

  def logo
    send_file Rails.root.join("public", "logo.png"), type: "image/png", disposition: "inline"
  end

end
