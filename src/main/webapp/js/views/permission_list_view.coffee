
window.PermissionListView = Backbone.View.extend
  el: $ 'body'

  initialize: ->
    _.bindAll @

    @collection = new ItemList()
    @collection.bind 'add', @appendItem

    @counter = 0
    @render()

  render: -> 
    $(@el).append "<button>Add List Item</button>"
    $(@el).append "<ul></ul>"

  addItem: -> 
    @counter++

    item = new Item
    item.set part2: "#{item.get 'part2'} #{@counter}"
    @collection.add item

  appendItem: (item) -> 
    $('ul').append "<li>#{item.get 'part1'} #{item.get 'part2'}!</li>"

  events: 'click button': 'addItem'

