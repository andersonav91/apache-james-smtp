Rails.application.routes.draw do
  get 'webhooks/sendgrid'
  post 'webhooks/sendgrid'

  get 'webhooks/ses'
  post 'webhooks/ses'

  get 'webhooks/test'

  get 'logo', to: 'subscriptions#logo'

  get 'unsubscribe/:uuid', to: 'subscriptions#unsubscribe'

  # For details on the DSL available within this file, see http://guides.rubyonrails.org/routing.html
end
