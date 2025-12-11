(function (w) {
  var origin = window.location.origin || '';

  w.ENV = {
    API_BASE_URL: origin, // 항상 현재 페이지 origin 기준으로 호출
    STATIC_BASE_PATH: '/'
  };
})(window);