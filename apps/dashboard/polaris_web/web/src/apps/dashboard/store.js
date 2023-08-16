import {create} from "zustand"
import {devtools} from "zustand/middleware"

let store = (set)=>({
    leftNavSelected: '',
    setLeftNavSelected: (selected) =>  set({ leftNavSelected: selected }), 
    leftNavCollapsed: false,
    toggleLeftNavCollapsed: () => {
        set(state => ({ leftNavCollapsed: !state.leftNavCollapsed }))
    },
    username: window.USER_NAME,
    toastConfig: {
        isActive: false,
        isError: false,
        message: ""
    },
    setToastConfig: (updateToastConfig) => {
        set({
            toastConfig: {
                isActive: updateToastConfig.isActive,
                isError: updateToastConfig.isError,
                message: updateToastConfig.message
            }
        })
    },
    allCollections: [],
    setAllCollections:(allCollections)=>{
        set({allCollections: allCollections})
    },
    isLocalDeploy: window.DASHBOARD_MODE === "LOCAL_DEPLOY",

    allRoutes: [],
    setAllRoutes:(allRoutes)=>{
        set({allRoutes: allRoutes})
    },
})

store = devtools(store)
const Store = create(store)

export default Store

