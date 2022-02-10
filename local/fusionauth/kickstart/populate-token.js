function populate(jwt, user, registration) {
  // Conditionals support migration from not having a registraton to having one
  if (registration && registration.data) {
    if (registration.data.businessId) {
      jwt.businessId = registration.data.businessId;
    }
    if (registration.data.capitalUserId) {
      jwt.capitalUserId = registration.data.capitalUserId;
    }
    if (registration.data.userType) {
      jwt.userType = registration.data.userType;
    }
  }
  if (user.data) {
    if ((!jwt.businessId) && user.data.businessId) {
      jwt.businessId = user.data.businessId;
    }
    if ((!jwt.capitalUserId) && user.data.capitalUserId) {
      jwt.capitalUserId = user.data.capitalUserId;
    }
    if ((!jwt.userType) && user.data.userType) {
      jwt.userType = user.data.userType;
    }
  }
  if (registration && registration.id) {
    jwt.userId = registration.id;
  } else if (user.id) {
    jwt.userId = user.id;
  }
}