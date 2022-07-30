import React, { useState, useEffect } from 'react';

import { Handle, Position } from 'react-flow-renderer';
import Box from '@mui/material/Box'
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField'
import FormControl from '@mui/material/FormControl';
import InputArgumentsDialog from './InputArgumentsDialog.jsx'
import './start-node.css';
import useStore from './store'

const BlankNode = (nodeData) => {
    const endpointsList = useStore(state => state.endpointsList)
    const nodeEndpointMap = useStore(state => state.nodeEndpointMap)
    const addNodeEndpoint = useStore(state => state.addNodeEndpoint)
    const fetchSampleDataFunc = useStore(state => state.fetchSampleDataFunc)

    let [endpointDetails, setEndpointDetails] = React.useState(null)

    const onEndpointChange = (event, endpointData) => {
        addNodeEndpoint(nodeData.id, endpointData)
        console.log("onEndpointChange setting", endpointDetails)
        setEndpointDetails(endpointData)
        console.log("onEndpointChange set complete", endpointDetails)
    }


    const onEndpointInputChange = (event, newInputValue) => {
        let endpointData = nodeEndpointMap[nodeData.id]
        if (endpointData) {
            if (newInputValue !== (endpointData.method + " " + endpointData.endpoint)) {
                addNodeEndpoint(nodeData.id, null)
                setEndpointDetails(null)
            }
        }
        
    }

    return (
        <div className={"start-node " + ((endpointDetails && endpointDetails.method) ? "green-boundary" : "red-boundary") }>
            <Handle
                type="target"
                position={Position.Top}
                isConnectable={false}
                style={{
                    height: '13px',
                    width: '13px',
                    bottom: '-6px'
                }}
            >
            </Handle>

            <div style={{display: "flex", justifyContent: "space-between", position: "relative"}}>
                <span className="variable-name">{nodeData.id}</span>
                {<FormControl sx={{ m: 1, minWidth: 120 }} fullWidth>
                    <Autocomplete
                        options={endpointsList}
                        autoHighlight
                        disableClearable
                        openOnFocus={!endpointDetails}
                        freeSolo
                        onChange={onEndpointChange}
                        onInputChange={onEndpointInputChange}
                        getOptionLabel={option => {return option.method ? (option.method + " " + option.endpoint) : "";}}
                        renderOption={(props, option) => (
                            <Box  component="li" {...props}>
                                <span className="autocomplete-options ">
                                    <span method={option.method}>{option.method} </span>
                                    {option.endpoint}
                                </span>
                            </Box>
                        )}
                        renderInput={(params) => {
                            let method = "";
                            if (params.inputProps.value && params.inputProps.value.indexOf(" ") > -1) {
                                let origValue = params.inputProps.value
                                params.inputProps.value = origValue.split(" ")[1];
                                method = origValue.split(" ")[0];
                            }
                            return (<TextField
                              {...params}
                              placeholder="Select API"
                              InputProps={{
                                ...params.InputProps,
                                componentsProps: { input: params.inputProps },
                                autoComplete: 'new-password',
                                disableUnderline: true,
                                startAdornment: (
                                    <span className={"MuiInput-input " + method}>
                                        {method}
                                    </span>
                                )
                              }}
                              
                              variant="standard"
                            />)
                        }}
                        
                    ></Autocomplete>
                </FormControl>}
                {endpointDetails && <InputArgumentsDialog endpointDetails={endpointDetails} fetchSampleDataFunc={fetchSampleDataFunc} />}
            </div>      
            <Handle
                type="source"
                position={Position.Bottom}
                isConnectable={false}
                style={{
                    height: '13px',
                    width: '13px',
                    bottom: '-6px'
                }}
                isConnectable={true}
            >
            </Handle>
        </div>
    )
}

export default BlankNode