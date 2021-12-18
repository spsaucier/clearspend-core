function populate(jwt, user, registration) {
  if (user.data) {
    if (user.data.businessId) {
      jwt.businessId = user.data.businessId;
    }
    if (user.data.capitalUserId) {
      jwt.capitalUserId = user.data.capitalUserId;
    }
    if (user.data.userType) {
      jwt.userType = user.data.userType;
    }
  }
  if (user.id) {
    jwt.userId = user.id;
  }
}